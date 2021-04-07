# Media
媒体库

# 引入

```
implementation 'cn.com.lasong:meida:latest.release'
```

## v0.0.1
* 添加MP3编解码线程与播放器, 实现边解码边播放  
* 添加双声道转单声道/单声道重采样/混音 动态库
	* 基于SpeexDst实现重采样
	* 后面有需要再引入降噪,增益等功能

## v0.0.2
* 把动态库加入到jniLibs

## v0.0.3
* 添加arm64-v8a支持

## v0.0.4
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