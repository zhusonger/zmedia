package cn.com.lasong.media;

/**
 * 音频排布
 */
public class AVChannelLayout {
    public static final long AV_CH_FRONT_LEFT             =0x00000001;
    public static final long AV_CH_FRONT_RIGHT            =0x00000002;
    public static final long AV_CH_FRONT_CENTER           =0x00000004;
    public static final long AV_CH_LOW_FREQUENCY          =0x00000008;
    public static final long AV_CH_BACK_LEFT              =0x00000010;
    public static final long AV_CH_BACK_RIGHT             =0x00000020;
    public static final long AV_CH_FRONT_LEFT_OF_CENTER   =0x00000040;
    public static final long AV_CH_FRONT_RIGHT_OF_CENTER  =0x00000080;
    public static final long AV_CH_BACK_CENTER            =0x00000100;
    public static final long AV_CH_SIDE_LEFT              =0x00000200;
    public static final long AV_CH_SIDE_RIGHT             =0x00000400;
    public static final long AV_CH_TOP_CENTER             =0x00000800;
    public static final long AV_CH_TOP_FRONT_LEFT         =0x00001000;
    public static final long AV_CH_TOP_FRONT_CENTER       =0x00002000;
    public static final long AV_CH_TOP_FRONT_RIGHT        =0x00004000;
    public static final long AV_CH_TOP_BACK_LEFT          =0x00008000;
    public static final long AV_CH_TOP_BACK_CENTER        =0x00010000;
    public static final long AV_CH_TOP_BACK_RIGHT         =0x00020000;
    public static final long AV_CH_STEREO_LEFT            =0x20000000;  ///< Stereo downmix.
    public static final long AV_CH_STEREO_RIGHT           =0x40000000;  ///< See AV_CH_STEREO_LEFT.

/** Channel mask value used for AVCodecContext.request_channel_layout
 to indicate that the user requests the channel order of the decoder output
 to be the native codec channel order. */
//  public static final long AV_CH_LAYOUT_NATIVE          0x8000000000000000ULL

    /**
     * @}
     * @defgroup channel_mask_c Audio channel layouts
     * @{
     * */
      public static final long AV_CH_LAYOUT_MONO              =(AV_CH_FRONT_CENTER);
      public static final long AV_CH_LAYOUT_STEREO            =(AV_CH_FRONT_LEFT|AV_CH_FRONT_RIGHT);
      public static final long AV_CH_LAYOUT_2POINT1           =(AV_CH_LAYOUT_STEREO|AV_CH_LOW_FREQUENCY);
      public static final long AV_CH_LAYOUT_2_1               =(AV_CH_LAYOUT_STEREO|AV_CH_BACK_CENTER);
      public static final long AV_CH_LAYOUT_SURROUND          =(AV_CH_LAYOUT_STEREO|AV_CH_FRONT_CENTER);
      public static final long AV_CH_LAYOUT_3POINT1           =(AV_CH_LAYOUT_SURROUND|AV_CH_LOW_FREQUENCY);
      public static final long AV_CH_LAYOUT_4POINT0           =(AV_CH_LAYOUT_SURROUND|AV_CH_BACK_CENTER);
      public static final long AV_CH_LAYOUT_4POINT1           =(AV_CH_LAYOUT_4POINT0|AV_CH_LOW_FREQUENCY);
      public static final long AV_CH_LAYOUT_2_2               =(AV_CH_LAYOUT_STEREO|AV_CH_SIDE_LEFT|AV_CH_SIDE_RIGHT);
      public static final long AV_CH_LAYOUT_QUAD              =(AV_CH_LAYOUT_STEREO|AV_CH_BACK_LEFT|AV_CH_BACK_RIGHT);
      public static final long AV_CH_LAYOUT_5POINT0           =(AV_CH_LAYOUT_SURROUND|AV_CH_SIDE_LEFT|AV_CH_SIDE_RIGHT);
      public static final long AV_CH_LAYOUT_5POINT1           =(AV_CH_LAYOUT_5POINT0|AV_CH_LOW_FREQUENCY);
      public static final long AV_CH_LAYOUT_5POINT0_BACK      =(AV_CH_LAYOUT_SURROUND|AV_CH_BACK_LEFT|AV_CH_BACK_RIGHT);
      public static final long AV_CH_LAYOUT_5POINT1_BACK      =(AV_CH_LAYOUT_5POINT0_BACK|AV_CH_LOW_FREQUENCY);
      public static final long AV_CH_LAYOUT_6POINT0           =(AV_CH_LAYOUT_5POINT0|AV_CH_BACK_CENTER);
      public static final long AV_CH_LAYOUT_6POINT0_FRONT     =(AV_CH_LAYOUT_2_2|AV_CH_FRONT_LEFT_OF_CENTER|AV_CH_FRONT_RIGHT_OF_CENTER);
      public static final long AV_CH_LAYOUT_HEXAGONAL         =(AV_CH_LAYOUT_5POINT0_BACK|AV_CH_BACK_CENTER);
      public static final long AV_CH_LAYOUT_6POINT1           =(AV_CH_LAYOUT_5POINT1|AV_CH_BACK_CENTER);
      public static final long AV_CH_LAYOUT_6POINT1_BACK      =(AV_CH_LAYOUT_5POINT1_BACK|AV_CH_BACK_CENTER);
      public static final long AV_CH_LAYOUT_6POINT1_FRONT     =(AV_CH_LAYOUT_6POINT0_FRONT|AV_CH_LOW_FREQUENCY);
      public static final long AV_CH_LAYOUT_7POINT0           =(AV_CH_LAYOUT_5POINT0|AV_CH_BACK_LEFT|AV_CH_BACK_RIGHT);
      public static final long AV_CH_LAYOUT_7POINT0_FRONT     =(AV_CH_LAYOUT_5POINT0|AV_CH_FRONT_LEFT_OF_CENTER|AV_CH_FRONT_RIGHT_OF_CENTER);
      public static final long AV_CH_LAYOUT_7POINT1           =(AV_CH_LAYOUT_5POINT1|AV_CH_BACK_LEFT|AV_CH_BACK_RIGHT);
      public static final long AV_CH_LAYOUT_7POINT1_WIDE      =(AV_CH_LAYOUT_5POINT1|AV_CH_FRONT_LEFT_OF_CENTER|AV_CH_FRONT_RIGHT_OF_CENTER);
      public static final long AV_CH_LAYOUT_7POINT1_WIDE_BACK =(AV_CH_LAYOUT_5POINT1_BACK|AV_CH_FRONT_LEFT_OF_CENTER|AV_CH_FRONT_RIGHT_OF_CENTER);
      public static final long AV_CH_LAYOUT_OCTAGONAL         =(AV_CH_LAYOUT_5POINT0|AV_CH_BACK_LEFT|AV_CH_BACK_CENTER|AV_CH_BACK_RIGHT);
    //  public static final long AV_CH_LAYOUT_HEXADECAGONAL     =(AV_CH_LAYOUT_OCTAGONAL|AV_CH_WIDE_LEFT|AV_CH_WIDE_RIGHT|AV_CH_TOP_BACK_LEFT|AV_CH_TOP_BACK_RIGHT|AV_CH_TOP_BACK_CENTER|AV_CH_TOP_FRONT_CENTER|AV_CH_TOP_FRONT_LEFT|AV_CH_TOP_FRONT_RIGHT);
      public static final long AV_CH_LAYOUT_STEREO_DOWNMIX    =(AV_CH_STEREO_LEFT|AV_CH_STEREO_RIGHT);
}
