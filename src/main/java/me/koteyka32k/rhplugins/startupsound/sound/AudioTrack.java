package me.koteyka32k.rhplugins.startupsound.sound;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeType;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;

import static org.lwjgl.BufferUtils.createByteBuffer;
import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.openal.EXTThreadLocalContext.*;
import static org.lwjgl.openal.SOFTDirectChannels.AL_DIRECT_CHANNELS_SOFT;
import static org.lwjgl.stb.STBVorbis.*;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_get_samples_short_interleaved;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * OpenAL wrapper for playing audio straight from file.
 *
 * @author Koteyka32k
 * @since 1.0
 */
final class AudioTrack implements AutoCloseable {
    private static final int BUFFER_SIZE = 1024 * 8;

    private final VorbisTrack track;
    private final int format;
    private final long device;
    private final long context;
    private final int source;
    private final IntBuffer buffers;
    private final ShortBuffer pcm;
    long bufferOffset;

    public AudioTrack(File file) {
        this.track = new VorbisTrack(file);
        switch (track.channels) {
            case 1:
                this.format = AL_FORMAT_MONO16;
                break;
            case 2:
                this.format = AL_FORMAT_STEREO16;
                break;
            default:
                throw new UnsupportedOperationException("Unsupported number of channels: " + track.channels);
        }

        device = alcOpenDevice((ByteBuffer)null);
        if (device == NULL)
            throw new IllegalStateException("Failed to open the default device.");

        context = alcCreateContext(device, (IntBuffer)null);
        if (context == NULL)
            throw new IllegalStateException("Failed to create an OpenAL context.");

        this.pcm = memAllocShort(BUFFER_SIZE);

        alcSetThreadContext(context);

        ALCCapabilities deviceCaps = ALC.createCapabilities(device);
        AL.createCapabilities(deviceCaps);

        source = alGenSources();
        alSourcei(source, AL_DIRECT_CHANNELS_SOFT, AL_TRUE);

        buffers = memAllocInt(2);
        alGenBuffers(buffers);
    }

    @Override
    public void close() {
        alDeleteBuffers(buffers);
        alDeleteSources(source);

        memFree(buffers);
        memFree(pcm);

        alcSetThreadContext(NULL);
        alcDestroyContext(context);
        alcCloseDevice(device);
    }

    private int stream(int buffer) {
        int samples = 0;

        while (samples < BUFFER_SIZE) {
            pcm.position(samples);
            int samplesPerChannel = track.getSamplesShortInterleaved(pcm);
            if (samplesPerChannel == 0)
                break;

            samples += samplesPerChannel * track.channels;
        }

        if (samples != 0) {
            pcm.position(0);
            pcm.limit(samples);
            alBufferData(buffer, format, pcm, track.sampleRate);
            pcm.limit(BUFFER_SIZE);
        }

        return samples;
    }

    void play() {
        for (int i = 0; i < buffers.limit(); i++) {
            if (stream(buffers.get(i)) == 0)
                return;
        }

        alSourceQueueBuffers(source, buffers);
        alSourcePlay(source);
    }

    boolean update() {
        int processed = alGetSourcei(source, AL_BUFFERS_PROCESSED);

        for (int i = 0; i < processed; i++) {
            bufferOffset += BUFFER_SIZE / track.channels;

            int buffer = alSourceUnqueueBuffers(source);

            if (stream(buffer) == 0)
                return false;

            alSourceQueueBuffers(source, buffer);
        }

        if (processed == 2)
            alSourcePlay(source);

        return true;
    }

    /**
     * The Vorbis wrapper needed for playing audio.
     *
     * @author Koteyka32k
     * @since 1.0
     */
    private static class VorbisTrack implements AutoCloseable {
        private final ByteBuffer encodedAudio;
        private final long handle;
        private final int channels;
        private final int sampleRate;

        VorbisTrack(File file) {
            try {
                encodedAudio = fileToByteBuf(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try (MemoryStack stack = stackPush()) {
                IntBuffer error = stack.mallocInt(1);
                handle = stb_vorbis_open_memory(encodedAudio, error, null);
                if (handle == NULL)
                    throw new RuntimeException("Failed to open the audio file " + file.getName() + "! Error: " + error.get(0));

                STBVorbisInfo info = STBVorbisInfo.malloc(stack);
                init_info_struct(info.address());
                this.channels = info.channels();
                this.sampleRate = info.sample_rate();
            }
        }

        @Override
        public void close() {
            stb_vorbis_close(handle);
        }

        synchronized int getSamplesShortInterleaved(ShortBuffer pcm) {
            return stb_vorbis_get_samples_short_interleaved(handle, channels, pcm);
        }

        private void init_info_struct(@NativeType("stb_vorbis *") long ptr) {
            nstb_vorbis_get_info(handle, ptr);
        }

        @SuppressWarnings("all")
        private static ByteBuffer fileToByteBuf(File file) throws IOException {
            try (SeekableByteChannel fc = Files.newByteChannel(file.toPath())) {
                ByteBuffer buffer = createByteBuffer((int)fc.size() + 1);
                while (fc.read(buffer) != -1);
                buffer.flip();
                return memSlice(buffer);
            }
        }
    }
}