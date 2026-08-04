#!/usr/bin/env python3
"""Patch wasmtime4j linker.rs for CM host resources.

1) U32 rep <-> ResourceAny marshalling in sync host callbacks
2) Multi-resource registration per interface (wasmtime instance() is once-only)
3) Re-attach resources when function registration shadows the interface instance
"""
from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
LINKER = ROOT / ".deps" / "wasmtime4j" / "wasmtime4j-native" / "src" / "component" / "linker.rs"

HELPER = r'''
/// Resolve host_dynamic payload id for a ResourceType by probing known small ids.
fn host_dynamic_payload(rt: ResourceType) -> Option<u32> {
    for id in 1..512u32 {
        if ResourceType::host_dynamic(id) == rt {
            return Some(id);
        }
    }
    None
}

fn component_value_as_u32(cv: &ComponentValue) -> Result<u32, wasmtime::Error> {
    match cv {
        ComponentValue::U32(v) => Ok(*v),
        ComponentValue::S32(v) => Ok(*v as u32),
        ComponentValue::U64(v) => Ok(*v as u32),
        ComponentValue::S64(v) => Ok(*v as u32),
        ComponentValue::Own(v) | ComponentValue::Borrow(v) => Ok(*v as u32),
        other => Err(wasmtime::Error::msg(format!(
            "Expected integer resource rep, got {:?}",
            other
        ))),
    }
}

/// Convert host-callback params, mapping Resource handles to U32(rep) for Java.
fn vals_to_host_params(
    mut store: StoreContextMut<'_, ComponentStoreData>,
    params: &[Val],
) -> Result<Vec<ComponentValue>, wasmtime::Error> {
    let mut out = Vec::with_capacity(params.len());
    for param in params {
        match param {
            Val::Resource(resource) => {
                let dyn_res = ResourceDynamic::try_from_resource_any(*resource, &mut store)
                    .map_err(|e| {
                        wasmtime::Error::msg(format!(
                            "Failed to extract host resource rep from param: {}",
                            e
                        ))
                    })?;
                out.push(ComponentValue::U32(dyn_res.rep()));
            }
            other => out.push(val_to_component_value(other)),
        }
    }
    Ok(out)
}

/// Convert host-callback results, creating ResourceAny when the result type is own/borrow.
fn host_results_to_vals(
    mut store: StoreContextMut<'_, ComponentStoreData>,
    func_type: ComponentFunc,
    cv_results: &[ComponentValue],
    results: &mut [Val],
) -> Result<(), wasmtime::Error> {
    let result_types: Vec<Type> = func_type.results().collect();
    for (i, cv) in cv_results.iter().enumerate() {
        if i >= results.len() {
            break;
        }
        match result_types.get(i) {
            Some(Type::Own(rt)) => {
                let rep = component_value_as_u32(cv)?;
                let payload = host_dynamic_payload(*rt).ok_or_else(|| {
                    wasmtime::Error::msg(format!(
                        "Unknown host_dynamic resource type for own result[{i}]"
                    ))
                })?;
                let dyn_res = ResourceDynamic::new_own(rep, payload);
                results[i] = Val::Resource(dyn_res.try_into_resource_any(&mut store).map_err(
                    |e| {
                        wasmtime::Error::msg(format!(
                            "Failed to create owned resource result: {}",
                            e
                        ))
                    },
                )?);
            }
            Some(Type::Borrow(rt)) => {
                let rep = component_value_as_u32(cv)?;
                let payload = host_dynamic_payload(*rt).ok_or_else(|| {
                    wasmtime::Error::msg(format!(
                        "Unknown host_dynamic resource type for borrow result[{i}]"
                    ))
                })?;
                let dyn_res = ResourceDynamic::new_borrow(rep, payload);
                results[i] = Val::Resource(dyn_res.try_into_resource_any(&mut store).map_err(
                    |e| {
                        wasmtime::Error::msg(format!(
                            "Failed to create borrowed resource result: {}",
                            e
                        ))
                    },
                )?);
            }
            _ => {
                results[i] = component_value_to_val(cv).map_err(|e| {
                    wasmtime::Error::msg(format!("Failed to convert result value: {}", e))
                })?;
            }
        }
    }
    Ok(())
}

'''

SYNC_BODY = '''move |mut store_ctx: StoreContextMut<'_, ComponentStoreData>,
                          func_type: ComponentFunc,
                          params: &[Val],
                          results: &mut [Val]| {
                        let cv_params = vals_to_host_params(store_ctx.as_context_mut(), params)?;
                        let cv_results =
                            CALLBACK.callback.execute(&cv_params).map_err(|e| {
                                wasmtime::Error::msg(format!(
                                    "Host function callback failed: {}",
                                    e
                                ))
                            })?;
                        host_results_to_vals(store_ctx, func_type, &cv_results, results)?;
                        Ok(())
                    }'''

RESOURCE_REGISTRY = r'''
/// Process-global host resource definitions (mirrors host-function registry).
/// Needed because nativeInstantiateWithLinker rebuilds a fresh linker via
/// add_registered_host_functions_to_linker and would otherwise drop resources.
type HostResourceRegistry = HashMap<
    String,
    Vec<(String, u32, Arc<dyn ResourceDestructorCallback>)>,
>;

fn get_component_host_resource_registry() -> &'static Mutex<HostResourceRegistry> {
    static REGISTRY: std::sync::OnceLock<Mutex<HostResourceRegistry>> = std::sync::OnceLock::new();
    REGISTRY.get_or_init(|| Mutex::new(HashMap::new()))
}

fn register_host_resource_globally(
    interface_path: &str,
    resource_name: &str,
    resource_id: u32,
    destructor: Arc<dyn ResourceDestructorCallback>,
) {
    if let Ok(mut registry) = get_component_host_resource_registry().lock() {
        let entry = registry.entry(interface_path.to_string()).or_default();
        entry.retain(|(n, _, _)| n != resource_name);
        entry.push((resource_name.to_string(), resource_id, destructor));
    }
}

'''

DEFINE_RESOURCE_NEW = r'''pub fn define_resource(
        &mut self,
        interface_path: &str,
        resource_name: &str,
        resource_id: u32,
        destructor: Arc<dyn ResourceDestructorCallback>,
    ) -> WasmtimeResult<()> {
        // Track then re-register: LinkerInstance::instance() inserts once per name.
        register_host_resource_globally(
            interface_path,
            resource_name,
            resource_id,
            destructor.clone(),
        );
        self.interface_resource_entries
            .entry(interface_path.to_string())
            .or_default()
            .push((resource_name.to_string(), resource_id, destructor));

        let resources: Vec<(String, u32, Arc<dyn ResourceDestructorCallback>)> = self
            .interface_resource_entries
            .get(interface_path)
            .cloned()
            .unwrap_or_default();
        let functions: Vec<(String, Arc<ComponentHostFunctionEntry>, bool)> = self
            .interface_function_entries
            .get(interface_path)
            .cloned()
            .unwrap_or_default();

        self.linker.allow_shadowing(true);
        let mut root = self.linker.root();
        let mut inst = root.instance(interface_path).map_err(|e| WasmtimeError::Linker {
            message: format!(
                "Failed to get linker instance for '{}': {}",
                interface_path, e
            ),
        })?;

        for (res_name, res_id, dtor) in resources {
            let resource_type = ResourceType::host_dynamic(res_id);
            inst.resource(&res_name, resource_type, move |_store_ctx, rep| {
                dtor.destroy(rep)
                    .map_err(|e| wasmtime::Error::msg(format!("Resource destructor failed: {}", e)))
            })
            .map_err(|e| WasmtimeError::Linker {
                message: format!(
                    "Failed to define resource '{}' on '{}': {}",
                    res_name, interface_path, e
                ),
            })?;
        }

        for (fn_name, fn_entry, fn_is_async) in functions {
            if fn_is_async {
                let fn_entry_clone = fn_entry.clone();
                inst.func_new_async(
                    &fn_name,
                    move |_store_ctx: StoreContextMut<'_, ComponentStoreData>,
                          _func_type,
                          params: &[Val],
                          results: &mut [Val]| {
                        let entry = fn_entry_clone.clone();
                        Box::new(async move {
                            let cv_params: Vec<ComponentValue> =
                                params.iter().map(val_to_component_value).collect();
                            let cv_results =
                                entry.callback.execute(&cv_params).map_err(|e| {
                                    wasmtime::Error::msg(format!(
                                        "Host function callback failed: {}",
                                        e
                                    ))
                                })?;
                            for (i, cv) in cv_results.iter().enumerate() {
                                if i < results.len() {
                                    results[i] = component_value_to_val(cv).map_err(|e| {
                                        wasmtime::Error::msg(format!(
                                            "Failed to convert result value: {}",
                                            e
                                        ))
                                    })?;
                                }
                            }
                            Ok(())
                        })
                    },
                )
                .map_err(|e| WasmtimeError::Linker {
                    message: format!(
                        "Failed to register async function '{}' on '{}': {}",
                        fn_name, interface_path, e
                    ),
                })?;
            } else {
                let fn_entry_clone = fn_entry.clone();
                inst.func_new(
                    &fn_name,
                    move |mut store_ctx: StoreContextMut<'_, ComponentStoreData>,
                          func_type: ComponentFunc,
                          params: &[Val],
                          results: &mut [Val]| {
                        let cv_params = vals_to_host_params(store_ctx.as_context_mut(), params)?;
                        let cv_results =
                            fn_entry_clone.callback.execute(&cv_params).map_err(|e| {
                                wasmtime::Error::msg(format!(
                                    "Host function callback failed: {}",
                                    e
                                ))
                            })?;
                        host_results_to_vals(store_ctx, func_type, &cv_results, results)?;
                        Ok(())
                    },
                )
                .map_err(|e| WasmtimeError::Linker {
                    message: format!(
                        "Failed to register function '{}' on '{}': {}",
                        fn_name, interface_path, e
                    ),
                })?;
            }
        }

        self.linker.allow_shadowing(false);

        let interface_key = interface_path.to_string();
        let resource_entry = format!("[resource]{}", resource_name);
        self.defined_interfaces
            .entry(interface_key)
            .or_default()
            .push(resource_entry);

        Ok(())
    }'''


def patch_sync_closure(text: str, callback_expr: str) -> tuple[str, int]:
    pattern = re.compile(
        r"move \|_store_ctx: StoreContextMut<'_, ComponentStoreData>,\s*"
        r"_func_type,\s*"
        r"params: &\[Val\],\s*"
        r"results: &mut \[Val\]\| \{\s*"
        r"let cv_params: Vec(?:<ComponentValue>)? =\s*"
        r"params\.iter\(\)\.map\(val_to_component_value\)\.collect\(\);\s*"
        r"let cv_results =\s*"
        + re.escape(callback_expr)
        + r"\.callback\.execute\(&cv_params\)\.map_err\(\|e\| \{\s*"
        r"wasmtime::Error::msg\(format!\(\s*"
        r'"Host function callback failed: \{\}",\s*'
        r"e\s*"
        r"\)\s*"
        r"\}\)\?;\s*"
        r"for \(i, cv\) in cv_results\.iter\(\)\.enumerate\(\) \{\s*"
        r"if i < results\.len\(\) \{\s*"
        r"results\[i\] = component_value_to_val\(cv\)\.map_err\(\|e\| \{\s*"
        r"wasmtime::Error::msg\(format!\(\s*"
        r'"Failed to convert result value: \{\}",\s*'
        r"e\s*"
        r"\)\s*"
        r"\}\)\?;\s*"
        r"\}\s*"
        r"\}\s*"
        r"Ok\(\(\)\)\s*"
        r"\}",
        re.MULTILINE,
    )
    body = SYNC_BODY.replace("CALLBACK", callback_expr)
    new_text, n = pattern.subn(body, text)
    return new_text, n


def ensure_resource_entries_field(text: str) -> tuple[str, bool]:
    if "interface_resource_entries" in text:
        return text, False
    old = (
        "    /// Entries per interface for re-registration when adding multiple functions\n"
        "    interface_function_entries: HashMap<String, Vec<(String, Arc<ComponentHostFunctionEntry>, bool)>>,"
    )
    new = (
        "    /// Entries per interface for re-registration when adding multiple functions\n"
        "    interface_function_entries: HashMap<String, Vec<(String, Arc<ComponentHostFunctionEntry>, bool)>>,\n"
        "    /// Resource types per interface (re-registered with functions under allow_shadowing)\n"
        "    interface_resource_entries: HashMap<String, Vec<(String, u32, Arc<dyn ResourceDestructorCallback>)>>,"
    )
    if old not in text:
        raise RuntimeError("interface_function_entries field marker not found")
    return text.replace(old, new, 1), True


def ensure_resource_entries_inits(text: str) -> tuple[str, int]:
    if text.count("interface_resource_entries: HashMap::new()") >= 3:
        return text, 0
    old = (
        "            interface_function_entries: HashMap::new(),\n"
        "            wasi_p2_enabled: false,"
    )
    new = (
        "            interface_function_entries: HashMap::new(),\n"
        "            interface_resource_entries: HashMap::new(),\n"
        "            wasi_p2_enabled: false,"
    )
    n = text.count(old)
    if n == 0:
        raise RuntimeError("ComponentLinker init sites not found")
    return text.replace(old, new), n


def ensure_global_resource_registry(text: str) -> tuple[str, bool]:
    if "get_component_host_resource_registry" in text:
        return text, False
    marker = "pub fn get_component_host_function_registry("
    idx = text.find(marker)
    if idx < 0:
        marker = "fn get_component_host_function_registry("
        idx = text.find(marker)
    if idx < 0:
        raise RuntimeError("host function registry marker not found")
    return text[:idx] + RESOURCE_REGISTRY + text[idx:], True


def patch_add_registered_resources(text: str) -> tuple[str, bool]:
    marker = "// WASI_WEBGPU_CM: attach global host resources"
    if marker in text:
        return text, False
    needle = (
        "    if by_iface.is_empty() {\n"
        "        return Ok(());\n"
        "    }\n"
        "    linker.allow_shadowing(true);\n"
        "    for (iface, entries) in by_iface {\n"
        "        let mut root = linker.root();\n"
        "        let mut inst = root.instance(&iface).map_err(|e| WasmtimeError::Linker {\n"
        "            message: format!(\"Failed to get linker instance for '{}': {}\", iface, e),\n"
        "        })?;\n"
        "        for entry in entries {"
    )
    if needle not in text:
        raise RuntimeError("add_registered_host_functions_to_linker loop not found")
    insert = (
        "    let mut resource_ifaces: HashMap<\n"
        "        String,\n"
        "        Vec<(String, u32, Arc<dyn ResourceDestructorCallback>)>,\n"
        "    > = HashMap::new();\n"
        "    if let Ok(registry) = get_component_host_resource_registry().lock() {\n"
        "        for (iface, resources) in registry.iter() {\n"
        "            resource_ifaces.insert(iface.clone(), resources.clone());\n"
        "            by_iface.entry(iface.clone()).or_default();\n"
        "        }\n"
        "    }\n"
        "    if by_iface.is_empty() {\n"
        "        return Ok(());\n"
        "    }\n"
        "    linker.allow_shadowing(true);\n"
        "    for (iface, entries) in by_iface {\n"
        "        let mut root = linker.root();\n"
        "        let mut inst = root.instance(&iface).map_err(|e| WasmtimeError::Linker {\n"
        "            message: format!(\"Failed to get linker instance for '{}': {}\", iface, e),\n"
        "        })?;\n"
        f"        {marker}\n"
        "        if let Some(resources) = resource_ifaces.get(&iface) {\n"
        "            for (res_name, res_id, dtor) in resources {\n"
        "                let resource_type = ResourceType::host_dynamic(*res_id);\n"
        "                let dtor = dtor.clone();\n"
        "                inst.resource(res_name, resource_type, move |_store_ctx, rep| {\n"
        "                    dtor.destroy(rep).map_err(|e| {\n"
        "                        wasmtime::Error::msg(format!(\"Resource destructor failed: {}\", e))\n"
        "                    })\n"
        "                })\n"
        "                .map_err(|e| WasmtimeError::Linker {\n"
        "                    message: format!(\n"
        "                        \"Failed to define resource '{}' on '{}': {}\",\n"
        "                        res_name, iface, e\n"
        "                    ),\n"
        "                })?;\n"
        "            }\n"
        "        }\n"
        "        for entry in entries {"
    )
    return text.replace(needle, insert, 1), True


def patch_define_resource(text: str) -> tuple[str, bool]:
    if (
        "register_host_resource_globally(" in text
        and "for (res_name, res_id, dtor) in resources" in text
        and "self.interface_resource_entries" in text
    ):
        return text, False
    pattern = re.compile(
        r"pub fn define_resource\(\s*"
        r"&mut self,\s*"
        r"interface_path: &str,\s*"
        r"resource_name: &str,\s*"
        r"resource_id: u32,\s*"
        r"destructor: Arc<dyn ResourceDestructorCallback>,\s*"
        r"\) -> WasmtimeResult<\(\)> \{.*?"
        r"Ok\(\(\)\)\s*\}",
        re.DOTALL,
    )
    new_text, n = pattern.subn(DEFINE_RESOURCE_NEW, text, count=1)
    if n != 1:
        raise RuntimeError(f"define_resource replace count={n}")
    return new_text, True


def patch_register_func_resources(text: str) -> tuple[str, bool]:
    """When re-registering functions, also re-attach tracked resources first."""
    marker = "// WASI_WEBGPU_CM: re-register resources before functions"
    if marker in text:
        return text, False

    needle = (
        "        let mut root = self.linker.root();\n"
        "        let mut inst = root.instance(interface_path).map_err(|e| WasmtimeError::Linker {\n"
        "            message: format!(\n"
        "                \"Failed to get linker instance for '{}': {}\",\n"
        "                interface_path, e\n"
        "            ),\n"
        "        })?;\n"
        "\n"
        "        // Register ALL functions for this interface (re-registers existing + new)"
    )
    insert = (
        "        let mut root = self.linker.root();\n"
        "        let mut inst = root.instance(interface_path).map_err(|e| WasmtimeError::Linker {\n"
        "            message: format!(\n"
        "                \"Failed to get linker instance for '{}': {}\",\n"
        "                interface_path, e\n"
        "            ),\n"
        "        })?;\n"
        "\n"
        f"        {marker}\n"
        "        let resources: Vec<(String, u32, Arc<dyn ResourceDestructorCallback>)> = self\n"
        "            .interface_resource_entries\n"
        "            .get(interface_path)\n"
        "            .cloned()\n"
        "            .unwrap_or_default();\n"
        "        for (res_name, res_id, dtor) in resources {\n"
        "            let resource_type = ResourceType::host_dynamic(res_id);\n"
        "            inst.resource(&res_name, resource_type, move |_store_ctx, rep| {\n"
        "                dtor.destroy(rep).map_err(|e| {\n"
        "                    wasmtime::Error::msg(format!(\"Resource destructor failed: {}\", e))\n"
        "                })\n"
        "            })\n"
        "            .map_err(|e| WasmtimeError::Linker {\n"
        "                message: format!(\n"
        "                    \"Failed to define resource '{}' on '{}': {}\",\n"
        "                    res_name, interface_path, e\n"
        "                ),\n"
        "            })?;\n"
        "        }\n"
        "\n"
        "        // Register ALL functions for this interface (re-registers existing + new)"
    )
    if needle not in text:
        raise RuntimeError("register_func_on_linker insert point not found")
    # Only patch the first occurrence inside register_func_on_linker
    idx = text.find("fn register_func_on_linker")
    if idx < 0:
        raise RuntimeError("register_func_on_linker not found")
    pos = text.find(needle, idx)
    if pos < 0:
        raise RuntimeError("register_func_on_linker needle not found after fn")
    return text[:pos] + insert + text[pos + len(needle) :], True


def main() -> int:
    if not LINKER.exists():
        print(f"missing {LINKER}", file=sys.stderr)
        return 1
    text = LINKER.read_text(encoding="utf-8")

    if "fn host_dynamic_payload" not in text:
        marker = "/// Component Model linker for defining host functions and instantiating components\npub struct ComponentLinker {"
        if marker not in text:
            print("could not find ComponentLinker marker", file=sys.stderr)
            return 1
        text = text.replace(
            marker,
            HELPER
            + "/// Component Model linker for defining host functions and instantiating components\npub struct ComponentLinker {",
            1,
        )
        print("inserted helpers")
    else:
        print("helpers already present")

    old_imp = "Instance as ComponentInstance, InstancePre, Linker, ResourceTable, ResourceType, Val,"
    new_imp = (
        "types::ComponentFunc, Instance as ComponentInstance, InstancePre, Linker, "
        "ResourceDynamic, ResourceTable, ResourceType, Type, Val,"
    )
    wasm_use = text.split("use wasmtime::{", 1)[1].split("};", 1)[0]
    if "ResourceDynamic" not in wasm_use:
        if old_imp not in text:
            print("import line not found", file=sys.stderr)
            return 1
        text = text.replace(old_imp, new_imp, 1)
        print("updated imports")

    wasm_use = text.split("use wasmtime::{", 1)[1].split("};", 1)[0]
    if "AsContextMut" not in wasm_use:
        text = text.replace(
            "Engine as WasmtimeEngine, Store, StoreContextMut,",
            "AsContextMut, Engine as WasmtimeEngine, Store, StoreContextMut,",
            1,
        )
        print("added AsContextMut import")

    text, n1 = patch_sync_closure(text, "fn_entry_clone")
    text, n2 = patch_sync_closure(text, "fn_entry")
    print(f"patched sync closures: fn_entry_clone={n1}, fn_entry={n2}")

    text, added_field = ensure_resource_entries_field(text)
    print(f"resource entries field added={added_field}")
    text, n_init = ensure_resource_entries_inits(text)
    print(f"resource entries inits={n_init}")

    text, reg_added = ensure_global_resource_registry(text)
    print(f"global resource registry added={reg_added}")

    text, def_patched = patch_define_resource(text)
    print(f"define_resource rewritten={def_patched}")

    text, reg_patched = patch_register_func_resources(text)
    print(f"register_func resources reattach={reg_patched}")

    text, add_reg_patched = patch_add_registered_resources(text)
    print(f"add_registered resources attach={add_reg_patched}")

    LINKER.write_text(text, encoding="utf-8", newline="\n")
    print(f"wrote {LINKER}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
