from pathlib import Path

p = Path(
    r"C:\Users\clt\Desktop\project\playground\android\wasi-webgpu-jvm-mvp"
    r"\.deps\wasmtime4j\wasmtime4j-native\src\component\linker.rs"
)
text = p.read_text(encoding="utf-8")

old1 = """            inst.func_new(
                &fn_name,
                move |_store_ctx: StoreContextMut<'_, ComponentStoreData>,
                      _func_type,
                      params: &[Val],
                      results: &mut [Val]| {
                    let cv_params: Vec<ComponentValue> =
                        params.iter().map(val_to_component_value).collect();
                    let cv_results = fn_entry.callback.execute(&cv_params).map_err(|e| {
                        wasmtime::Error::msg(format!("Host function callback failed: {}", e))
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
                },
            )"""

new1 = """            inst.func_new(
                &fn_name,
                move |store_ctx: StoreContextMut<'_, ComponentStoreData>,
                      func_type: &ComponentFunc,
                      params: &[Val],
                      results: &mut [Val]| {
                    let cv_params = vals_to_host_params(store_ctx.as_context_mut(), params)?;
                    let cv_results = fn_entry.callback.execute(&cv_params).map_err(|e| {
                        wasmtime::Error::msg(format!("Host function callback failed: {}", e))
                    })?;
                    host_results_to_vals(store_ctx, func_type, &cv_results, results)?;
                    Ok(())
                },
            )"""

old2 = """                inst.func_new(
                    &fn_name,
                    move |_store_ctx: StoreContextMut<'_, ComponentStoreData>,
                          _func_type,
                          params: &[Val],
                          results: &mut [Val]| {
                        let cv_params: Vec<ComponentValue> =
                            params.iter().map(val_to_component_value).collect();
                        let cv_results =
                            fn_entry_clone.callback.execute(&cv_params).map_err(|e| {
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
                    },
                )"""

new2 = """                inst.func_new(
                    &fn_name,
                    move |store_ctx: StoreContextMut<'_, ComponentStoreData>,
                          func_type: &ComponentFunc,
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
                )"""

n1 = text.count(old1)
n2 = text.count(old2)
print("old1 count", n1, "old2 count", n2)
if n1 != 1 or n2 != 1:
    raise SystemExit(1)
p.write_text(text.replace(old1, new1).replace(old2, new2), encoding="utf-8", newline="\n")
print("patched both sync closures")
