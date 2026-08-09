package io.github.fenriliuguang.wasi.webgpu.experimental.host

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CpuTextureSamplerPipelineLayoutTest {

    @Test
    fun createTextureViewSamplerAndPipelineLayout() {
        CpuWasiWebGpuHost().use { host ->
            val adapter = host.requestAdapter()
            val device = host.adapterRequestDevice(adapter)

            val texture = host.deviceCreateTexture(
                device,
                TextureDescriptor(
                    size = Extent3D(width = 4, height = 4),
                    format = GpuTextureFormat.RGBA8_UNORM,
                    usage = GpuTextureUsage.TEXTURE_BINDING or GpuTextureUsage.COPY_DST,
                ),
            )
            val view = host.textureCreateView(texture)
            val sampler = host.deviceCreateSampler(device, SamplerDescriptor(label = "s"))

            val bgl = host.deviceCreateBindGroupLayout(
                device,
                BindGroupLayoutDescriptor(
                    entries = listOf(
                        BindGroupLayoutEntry(
                            binding = 0,
                            visibility = GpuShaderStage.FRAGMENT,
                            sampler = SamplerBindingLayout(GpuSamplerBindingType.FILTERING),
                        ),
                        BindGroupLayoutEntry(
                            binding = 1,
                            visibility = GpuShaderStage.FRAGMENT,
                            texture = TextureBindingLayout(
                                sampleType = GpuTextureSampleType.FLOAT,
                                viewDimension = GpuTextureViewDimension.D2,
                            ),
                        ),
                    ),
                ),
            )
            val bindGroup = host.deviceCreateBindGroup(
                device,
                BindGroupDescriptor(
                    layout = bgl,
                    entries = listOf(
                        BindGroupEntry(0, BindingResource.Sampler(sampler)),
                        BindGroupEntry(1, BindingResource.TextureView(view)),
                    ),
                ),
            )
            val pipelineLayout = host.deviceCreatePipelineLayout(
                device,
                PipelineLayoutDescriptor(bindGroupLayouts = listOf(bgl)),
            )

            assertNotEquals(0, texture.raw)
            assertNotEquals(0, view.raw)
            assertNotEquals(0, sampler.raw)
            assertNotEquals(0, bindGroup.raw)
            assertNotEquals(0, pipelineLayout.raw)
            assertTrue(host.tryDrop(pipelineLayout))
            assertTrue(host.tryDrop(bindGroup))
            assertTrue(host.tryDrop(view))
            assertTrue(host.tryDrop(texture))
            assertTrue(host.tryDrop(sampler))
        }
    }
}
