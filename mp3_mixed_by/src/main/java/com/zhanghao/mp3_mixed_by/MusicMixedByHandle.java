package com.zhanghao.mp3_mixed_by;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 处理混音
 * mp3_1-----> aac -----> PCM
 * ###########################-----> PCM ------> 混合后的PCM ----->WAV
 * MP3_2-----> aac -----> PCM
 */
public class MusicMixedByHandle implements Runnable {

    private static final String TAG = "zh___MusicMixedByHandle";

    private Context context;

    public MusicMixedByHandle(Context context) {
        this.context = context;
    }

    @SuppressLint("WrongConstant")
    @Override
    public void run() {
        try {
            File audio_1 = new File(context.getExternalFilesDir(null), "music1.mp3");
            File audio_2 = new File(context.getExternalFilesDir(null), "music2.mp3");

            // 总时长
            long duration_1 = getAudioFileDuration(audio_1) * 1000;
            long duration_2 = getAudioFileDuration(audio_2) * 1000;
            Log.i(TAG, "run: duration_1 = " + duration_1);
            Log.i(TAG, "run: duration_2 = " + duration_2);

            // PCM
            File audio_1_out = new File(context.getExternalFilesDir(null), "music_1_out.pcm");
            File audio_2_out = new File(context.getExternalFilesDir(null), "music_2_out.pcm");

            MediaExtractor mediaExtractor_1 = new MediaExtractor();
            MediaExtractor mediaExtractor_2 = new MediaExtractor();
            mediaExtractor_1.setDataSource(audio_1.getAbsolutePath());
            mediaExtractor_2.setDataSource(audio_2.getAbsolutePath());

            int audioTrackIndex_1 = getAudioTrackIndex(mediaExtractor_1);
            mediaExtractor_1.selectTrack(audioTrackIndex_1);
            int audioTrackIndex_2 = getAudioTrackIndex(mediaExtractor_2);
            mediaExtractor_2.selectTrack(audioTrackIndex_2);

            MediaFormat format_1 = mediaExtractor_1.getTrackFormat(audioTrackIndex_1);
            MediaFormat format_2 = mediaExtractor_2.getTrackFormat(audioTrackIndex_2);

            // 获取音频轨道的最大缓存大小，如果不指定，默认为100Kb
            int maxBufferSize_1 = 100 * 1024;
            int maxBufferSize_2 = 100 * 1024;
            if (format_1.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                maxBufferSize_1 = format_1.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
            }
            if (format_2.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                maxBufferSize_2 = format_2.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
            }

            ByteBuffer buffer_1 = ByteBuffer.allocateDirect(maxBufferSize_1);
            ByteBuffer buffer_2 = ByteBuffer.allocateDirect(maxBufferSize_2);

            // 开始用MediaCodec 解码 , 解码 audio_1
            MediaCodec decoder_1 = MediaCodec.createDecoderByType(format_1.getString(MediaFormat.KEY_MIME));
            decoder_1.configure(format_1, null, null, 0);
            decoder_1.start();
            MediaCodec.BufferInfo info_1 = new MediaCodec.BufferInfo();
            while (true) {
                int inputBufferIndex = decoder_1.dequeueInputBuffer(100000);
                if (inputBufferIndex >= 0) {
                    long sampleTime = mediaExtractor_1.getSampleTime();
                    if (sampleTime == -1) {
                        break;
                    } else if (sampleTime > duration_1) {
                        break;
                    }

                    info_1.size = mediaExtractor_1.readSampleData(buffer_1, 0);
                    info_1.presentationTimeUs = mediaExtractor_1.getSampleTime();
                    info_1.flags = mediaExtractor_1.getSampleFlags();
                    byte[] content = new byte[buffer_1.remaining()];
                    buffer_1.get(content);
                    ByteBuffer inputBuffer = decoder_1.getInputBuffer(inputBufferIndex);
                    inputBuffer.clear();
                    inputBuffer.put(content);
                    decoder_1.queueInputBuffer(inputBufferIndex, 0, info_1.size, info_1.presentationTimeUs, info_1.flags);
                    mediaExtractor_1.advance();
                }

                int outputBufferIndex = decoder_1.dequeueOutputBuffer(info_1, 10000);
                while (outputBufferIndex >= 0) {
                    ByteBuffer outputBuffer = decoder_1.getOutputBuffer(outputBufferIndex);
                    FileUtil.writeByteBufferToFile(outputBuffer, audio_1_out.getAbsolutePath());
                    decoder_1.releaseOutputBuffer(outputBufferIndex, false);
                    outputBufferIndex = decoder_1.dequeueOutputBuffer(info_1, 10000);
                }
            }
            Log.i(TAG, "run: audio_1 解码结束-----------------");
            mediaExtractor_1.release();
            decoder_1.stop();
            decoder_1.release();

            // 解码 audio_2
            MediaCodec decoder_2 = MediaCodec.createDecoderByType(format_2.getString(MediaFormat.KEY_MIME));
            decoder_2.configure(format_2, null, null, 0);
            decoder_2.start();
            MediaCodec.BufferInfo info_2 = new MediaCodec.BufferInfo();
            while (true) {
                int inputBufferIndex = decoder_2.dequeueInputBuffer(100000);
                if (inputBufferIndex >= 0) {
                    long sampleTime = mediaExtractor_2.getSampleTime();
                    if (sampleTime == -1) {
                        break;
                    } else if (sampleTime > duration_2) {
                        break;
                    }
                }
                info_2.size = mediaExtractor_2.readSampleData(buffer_2, 0);
                info_2.presentationTimeUs = mediaExtractor_2.getSampleTime();
                info_2.flags = mediaExtractor_2.getSampleFlags();
                byte[] content = new byte[buffer_2.remaining()];
                buffer_2.get(content);
                ByteBuffer inputBuffer = decoder_2.getInputBuffer(inputBufferIndex);
                inputBuffer.clear();
                inputBuffer.put(content);
                decoder_2.queueInputBuffer(inputBufferIndex, 0, info_2.size, info_2.presentationTimeUs, info_2.flags);
                mediaExtractor_2.advance();

                int outputBufferIndex = decoder_2.dequeueOutputBuffer(info_2, 10000);
                while (outputBufferIndex >= 0) {
                    ByteBuffer outputBuffer = decoder_2.getOutputBuffer(outputBufferIndex);
                    FileUtil.writeByteBufferToFile(outputBuffer, audio_2_out.getAbsolutePath());
                    decoder_2.releaseOutputBuffer(outputBufferIndex, false);
                    outputBufferIndex = decoder_2.dequeueOutputBuffer(info_2, 10000);
                }
            }
            mediaExtractor_2.release();
            decoder_2.stop();
            decoder_2.release();
            Log.i(TAG, "run: audio_2 解码结束----------------");

            /**
             * 开始混音  music_1_out.pcm  music_2_out.pcm
             * PCM  LRLR....
             * 混音原理基于线性叠加原理，即两个声音信号可以通过简单的相加操作来混合
             * 振幅决定音量大小， 如：pcm1Left * 0.5  表示声音减小一半
             */
            Log.i(TAG, "run: 开始将 两个PCM文件进行混合--------");
            File mixed_out_pcm = new File(context.getExternalFilesDir(null), "music12_mixed_out.pcm");
            FileInputStream fis1 = new FileInputStream(audio_1_out);
            FileInputStream fis2 = new FileInputStream(audio_2_out);
            FileOutputStream fos = new FileOutputStream(mixed_out_pcm);
            // 左右声道共四个字节
            byte[] buffer1 = new byte[4];
            byte[] buffer2 = new byte[4];
            // 取最短文件的长度
            while (fis1.read(buffer1) != -1 && fis2.read(buffer2) != -1) {
                short pcm1Left = ByteBuffer.wrap(buffer1, 0, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
                short pcm1Right = ByteBuffer.wrap(buffer1, 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
                short pcm2Left = ByteBuffer.wrap(buffer2, 0, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
                short pcm2Right = ByteBuffer.wrap(buffer2, 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();

                short mixedLeft = (short) Math.min(32767, (pcm1Left / 32768.0 + pcm2Left / 32768.0) * 32768);
                short mixedRight = (short) Math.min(32767, (pcm1Right / 32768.0 + pcm2Right / 32768.0) * 32768);

                // left
                fos.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(mixedLeft).array());
                // right
                fos.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(mixedRight).array());
            }
            Log.i(TAG, "run: 混音结束=======================");

            Log.i(TAG, "run: 输出为WAV文件");
            File targetWAVMusic = new File(context.getExternalFilesDir(null), "target_out_music.wav");
            PcmToWavUtil pcmToWavUtil = new PcmToWavUtil(44100, AudioFormat.CHANNEL_IN_STEREO,
                    2, AudioFormat.ENCODING_PCM_16BIT);
            pcmToWavUtil.pcmToWav(mixed_out_pcm.getAbsolutePath(), targetWAVMusic.getAbsolutePath());
            Log.i(TAG, "run: 输出为WAV文件结束");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取音频轨道索引
     *
     * @param mediaExtractor
     * @return
     */
    private int getAudioTrackIndex(MediaExtractor mediaExtractor) {
        for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
            MediaFormat trackFormat = mediaExtractor.getTrackFormat(i);
            if (trackFormat.getString(MediaFormat.KEY_MIME).startsWith("audio/")) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 获取音频文件的总时长
     *
     * @param audioFile 文件
     * @return
     */
    private long getAudioFileDuration(File audioFile) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(audioFile.toString());
            String durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return Long.parseLong(durationString);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

}
