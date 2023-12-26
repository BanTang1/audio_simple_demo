package com.zhanghao.music_clip;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * 音乐处理类
 */
public class MusicHandle implements Runnable {

    private static final String TAG = "zh___MusicHandle";
    private MediaExtractor mediaExtractor;
    private Context context;
    private long startTime, endTime;

    public MusicHandle(Context context, long startTime, long endTime) {
        this.mediaExtractor = new MediaExtractor();
        this.context = context;
        this.startTime = startTime;
        this.endTime = endTime;

    }

    @SuppressLint("WrongConstant")
    @Override
    public void run() {
        try {
            // 设置文件路径  mp3
            File file = new File(context.getExternalFilesDir(null), "music.mp3");
            mediaExtractor.setDataSource(file.getAbsolutePath());

            // 输出文件路径  PCM
            File targetPCMFile = new File(context.getExternalFilesDir(null), "music_clip.pcm");

            // 输出文件路径 wav
            File targetWavFile = new File(context.getExternalFilesDir(null), "music_clip.wav");

            // 选择音频轨道
            int trackIndex = -1;
            for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
                MediaFormat format = mediaExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio/")) {
                    trackIndex = i;
                }
            }
            if (trackIndex == -1) {
                return;
            }
            mediaExtractor.selectTrack(trackIndex);

            // seek到需要截取的起始位置
            mediaExtractor.seekTo(startTime, MediaExtractor.SEEK_TO_CLOSEST_SYNC);

            // 获取该轨道的配置信息
            MediaFormat audioFormat = mediaExtractor.getTrackFormat(trackIndex);

            // 获取音频轨道的最大缓存大小，如果不指定，默认为100Kb
            int maxBufferSize = 100 * 1024;
            if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                maxBufferSize = audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
            }

            ByteBuffer buffer = ByteBuffer.allocateDirect(maxBufferSize);

            // 开始用MediaCodec 解码
            MediaCodec decoder = MediaCodec.createDecoderByType(audioFormat.getString(MediaFormat.KEY_MIME));
            decoder.configure(audioFormat, null, null, 0);
            decoder.start();
            // 用于存放音频的基本信息
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            while (true) {
                int inputBufferIndex = decoder.dequeueInputBuffer(100000);
                if (inputBufferIndex >= 0) {
                    // 获取当前样本的时间戳
                    long sampleTime = mediaExtractor.getSampleTime();
                    if (sampleTime == -1) {
                        break;
                    } else if (sampleTime < startTime) {
                        // 跳过不需要的样本
                        mediaExtractor.advance();
                        continue;
                    } else if (sampleTime > endTime) {
                        break;
                    }

                    info.size = mediaExtractor.readSampleData(buffer, 0);
                    info.presentationTimeUs = mediaExtractor.getSampleTime();
                    info.flags = mediaExtractor.getSampleFlags();

                    // 准备放入缓冲区
                    byte[] content = new byte[buffer.remaining()];
                    buffer.get(content);

                    ByteBuffer inputBuffer = decoder.getInputBuffer(inputBufferIndex);
                    inputBuffer.clear();
                    inputBuffer.put(content);
                    decoder.queueInputBuffer(inputBufferIndex, 0, info.size, info.presentationTimeUs, info.flags);

                    // 移动到下一个样本  -- 与  mediaExtractor.readSampleData() 方法搭配
                    mediaExtractor.advance();
                }

                int outputBufferIndex = decoder.dequeueOutputBuffer(info, 10000);
                while (outputBufferIndex >= 0) {
                    ByteBuffer outputBuffer = decoder.getOutputBuffer(outputBufferIndex);
                    FileUtil.writeByteBufferToFile(outputBuffer, targetPCMFile.getAbsolutePath());
                    decoder.releaseOutputBuffer(outputBufferIndex, false);
                    outputBufferIndex = decoder.dequeueOutputBuffer(info, 10000);
                }
            }

            // 获取 采样率  声道配置 通道数 编码方式  , 有些音频文件可能没有明确的某些配置信息，如 声道配置信息 编码方式
//            int sampleRate = audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
//            int channelConfig = audioFormat.getInteger(MediaFormat.KEY_CHANNEL_MASK);
//            int channelCount = audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
//            int encoding = audioFormat.getInteger(MediaFormat.KEY_PCM_ENCODING);

            // 释放资源
            mediaExtractor.release();
            decoder.stop();
            decoder.release();


            // 生成最终剪切过的的音频文件  wav格式
//            PcmToWavUtil pcmToWavUtil = new PcmToWavUtil(sampleRate, channelConfig, channelCount, encoding);
            PcmToWavUtil pcmToWavUtil = new PcmToWavUtil(44100, AudioFormat.CHANNEL_IN_STEREO,
                    2, AudioFormat.ENCODING_PCM_16BIT);
            pcmToWavUtil.pcmToWav(targetPCMFile.getAbsolutePath(), targetWavFile.getAbsolutePath());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
