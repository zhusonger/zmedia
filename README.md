# Media
媒体库

# 引入

```
implementation 'com.github.zhusonger:zmeida:1.1.0'
```

## ffmpeg-marker

这个ffmpeg相关库是基于目前的功能精简版本, 如果项目中已经有ffmpeg, 并且已经支持对应功能

可以去除ffmpeg相关的库

基于[ffmpeg-android-maker](https://github.com/Javernaut/ffmpeg-android-maker)修改的ffmpeg编译脚本

支持了iOS的编译


## developing(开发中)

* 更新 FFmpeg Version n4.4
* remux剪辑方法修改为静态方法

```java
int ret = Muxer.remux(<input path>, <output path>,
          <start seconds>, <end seconds>,
          <metadata>, <rotate>);
```

* 新增基于ffmpeg软编实现的视频合成(音频还未实现)
    * 新增AAC编码
    * 新增libx264软编库
    * 新增libmp3lame软编库

    > 会增大包体, 2个常用架构总共大概8M左右(毕竟单个libx264就有2M)   

```java
Muxer muxer = new Muxer();
muxer.init("<output>");
muxer.add_video_stream(4000_000, 1280, 720);
// 源数据未RGBA, 源数据大小为 640x360, 会转换到编码器格式, 并放大到1280x720
muxer.scale_video(AVPixelFormat.AV_PIX_FMT_RGBA.ordinal(), 640, 360);
loop {
    muxer.write_video_frame(<a frame rgba array>);
    if (end) {
        break;
    }
}
// 写入结尾
muxer.write_video_frame(null);
muxer.stop();
```

## 1.1.0

* 新增视频&音乐剪辑功能

### 代码示例

```java
    Mux muxer = new Muxer();
    // 开始与结束时间是s, double类型
    // 默认保留metadata和旋转信息, 可传入false关闭
    int ret = muxer.remux(<input path>, <output path>,
     <start seconds>, <end seconds>,
     <metadata>, <rotate>);
    if (ret == 0) {
        // success
    } else {
        // error
    }
```

### 失败错误码

    AVERROR_BSF_NOT_FOUND = -1179861752
    AVERROR_BUG = -558323010
    AVERROR_DECODER_NOT_FOUND = -1128613112
    AVERROR_DEMUXER_NOT_FOUND = -1296385272
    AVERROR_ENCODER_NOT_FOUND = -1129203192
    AVERROR_EOF = -541478725
    AVERROR_EXIT = -1414092869
    AVERROR_FILTER_NOT_FOUND = -1279870712
    AVERROR_INVALIDDATA = -1094995529
    AVERROR_MUXER_NOT_FOUND = -1481985528
    AVERROR_OPTION_NOT_FOUND = -1414549496
    AVERROR_PATCHWELCOME = -1163346256
    AVERROR_PROTOCOL_NOT_FOUND = -1330794744
    AVERROR_STREAM_NOT_FOUND = -1381258232
    AVERROR_BUG2 = -541545794
    AVERROR_UNKNOWN = -1313558101


## 1.0.0
* 添加MP3编解码线程与播放器, 实现边解码边播放  
* 添加双声道转单声道/单声道重采样/混音 动态库
	* 基于SpeexDst实现重采样
	* 后面有需要再引入降噪,增益等功能
* 把动态库加入到jniLibs
* 添加arm64-v8a支持
* 重采样修改为ffmpeg实现, 移除双声道转单声道接口, 通过ffmpeg的重采样可以实现相同功能

### 代码示例

```java

// 创建实例对象
Resample resample = new Resample();  

// 初始化, 前面3个为输入PCM的参数, 后3个是输出PCM的参数  
resample.init(
AVChannelLayout.AV_CH_LAYOUT_STEREO, 
AVSampleFormat.AV_SAMPLE_FMT_S16.ordinal(), 44100,

AVChannelLayout.AV_CH_LAYOUT_MONO, 
AVSampleFormat.AV_SAMPLE_FMT_S16.ordinal(), 16000);    

loop:
	// buf为源PCM字节数组, length为buf中有效PCM的字节长度  
	int size = resample.resample(buf, length);  
	// 读取重采样之后的字节数组, 
	// out为用于获取重采样后的字节数组  
	// out的长度大于等于resample后得到的长度, 可以取一个相对安全的长度  
	// size为resample读取到的重采样字节的长度  
	int read_size = resample.read(out, size);
	// out现在就是重采样后的字节数组数据, 一般情况read_size == size

// 释放重采样实例(如果需要频繁使用, 可以只创建一个实例, 重新调用init方法)  
resample.release();	

```