package com.netflix.governator.internal;

public enum BinaryConstant {
    B0_1, B1_2, B2_4, B3_8, B4_16, B5_32, B6_64, B7_128, 
    B8_256, B9_512, B10_1024, B11_2048, B12_4096, B13_8192, B14_16384, B15_32768, B16_65536, 
    B17_131072, B18_262144, B19_524288, B20_1048576, B21_2097152, B22_4194304, B23_8388608, B24_16777216, 
    B25_33554432, B26_67108864, B27_134217728, B28_268435456, B29_536870912, B30_1073741824, B31_2147483648, B32_4294967296, 
    B33_8589934592, B34_17179869184, B35_34359738368, B36_68719476736, B37_137438953472, B38_274877906944, B39_549755813888, B40_1099511627776, B41_2199023255552, B42_4398046511104, 
    B43_8796093022208, B44_17592186044416, B45_35184372088832, B46_70368744177664, B47_140737488355328, B48_281474976710656, B49_562949953421312, B50_1125899906842624, B51_2251799813685248, B52_4503599627370496, B53_9007199254740992, B54_18014398509481984, B55_36028797018963968, B56_72057594037927936, 
    B57_144115188075855872, B58_288230376151711744, B59_576460752303423488, B60_1152921504606846976, B61_2305843009213693952, B62_4611686018427387904, B63_N9223372036854775808;

    private final long value;
    private final String msg;

    private BinaryConstant() {
        this.value = 1L << ordinal();
        this.msg = name() + "(2^" + ordinal() + "=" + value + ")";
    }

    public String toString() {
        return msg;
    }
    
    public static final int I0_1 = 1;
    public static final int I1_2 = 2;
    public static final int I2_4 = 4;
    public static final int I3_8 = 8;
    public static final int I4_16 = 16;
    public static final int I5_32 = 32;
    public static final int I6_64 = 64;
    public static final int I7_128 = 128;
    public static final int I8_256 = 256;
    public static final int I9_512 = 512;
    public static final int I10_1024 = 1024;
    public static final int I11_2048 = 2048;
    public static final int I12_4096 = 4096;
    public static final int I13_8192 = 8192;
    public static final int I14_16384 = 16384;
    public static final int I15_32768 = 32768;
    public static final int I16_65536 = 65536;
    public static final int I17_131072 = 131072;
    public static final int I18_262144 = 262144;
    public static final int I19_524288 = 524288;
    public static final int I20_1048576 = 1048576;
    public static final int I21_2097152 = 2097152;
    public static final int I22_4194304 = 4194304;
    public static final int I23_8388608 = 8388608;
    public static final int I24_16777216 = 16777216;
    public static final int I25_33554432 = 33554432;
    public static final int I26_67108864 = 67108864;
    public static final int I27_134217728 = 134217728;
    public static final int I28_268435456 = 268435456;
    public static final int I29_536870912 = 536870912;
    public static final int I30_1073741824 = 1073741824;
    public static final int I31_2147483648 = -2147483648;
    
    public static final long L0_1 = 1L;
    public static final long L1_2 = 2L;
    public static final long L2_4 = 4L;
    public static final long L3_8 = 8L;
    public static final long L4_16 = 16L;
    public static final long L5_32 = 32L;
    public static final long L6_64 = 64L;
    public static final long L7_128 = 128L;
    public static final long L8_256 = 256L;
    public static final long L9_512 = 512L;
    public static final long L10_1024 = 1024L;
    public static final long L11_2048 = 2048L;
    public static final long L12_4096 = 4096L;
    public static final long L13_8192 = 8192L;
    public static final long L14_16384 = 16384L;
    public static final long L15_32768 = 32768L;
    public static final long L16_65536 = 65536L;
    public static final long L17_131072 = 131072L;
    public static final long L18_262144 = 262144L;
    public static final long L19_524288 = 524288L;
    public static final long L20_1048576 = 1048576L;
    public static final long L21_2097152 = 2097152L;
    public static final long L22_4194304 = 4194304L;
    public static final long L23_8388608 = 8388608L;
    public static final long L24_16777216 = 16777216L;
    public static final long L25_33554432 = 33554432L;
    public static final long L26_67108864 = 67108864L;
    public static final long L27_134217728 = 134217728L;
    public static final long L28_268435456 = 268435456L;
    public static final long L29_536870912 = 536870912L;
    public static final long L30_1073741824 = 1073741824L;
    public static final long L31_2147483648 = 2147483648L;
    public static final long L32_4294967296 = 4294967296L;
    public static final long L33_8589934592 = 8589934592L;
    public static final long L34_17179869184 = 17179869184L;
    public static final long L35_34359738368 = 34359738368L;
    public static final long L36_68719476736 = 68719476736L;
    public static final long L37_137438953472 = 137438953472L;
    public static final long L38_274877906944 = 274877906944L;
    public static final long L39_549755813888 = 549755813888L;
    public static final long L40_1099511627776 = 1099511627776L;
    public static final long L41_2199023255552 = 2199023255552L;
    public static final long L42_4398046511104 = 4398046511104L;
    public static final long L43_8796093022208 = 8796093022208L;
    public static final long L44_17592186044416 = 17592186044416L;
    public static final long L45_35184372088832 = 35184372088832L;
    public static final long L46_70368744177664 = 70368744177664L;
    public static final long L47_140737488355328 = 140737488355328L;
    public static final long L48_281474976710656 = 281474976710656L;
    public static final long L49_562949953421312 = 562949953421312L;
    public static final long L50_1125899906842624 = 1125899906842624L;
    public static final long L51_2251799813685248 = 2251799813685248L;
    public static final long L52_4503599627370496 = 4503599627370496L;
    public static final long L53_9007199254740992 = 9007199254740992L;
    public static final long L54_18014398509481984 = 18014398509481984L;
    public static final long L55_36028797018963968 = 36028797018963968L;
    public static final long L56_72057594037927936 = 72057594037927936L;
    public static final long L57_144115188075855872 = 144115188075855872L;
    public static final long L58_288230376151711744 = 288230376151711744L;
    public static final long L59_576460752303423488 = 576460752303423488L;
    public static final long L60_1152921504606846976 = 1152921504606846976L;
    public static final long L61_2305843009213693952 = 2305843009213693952L;
    public static final long L62_4611686018427387904 = 4611686018427387904L;
    public static final long L63_N9223372036854775808 = -9223372036854775808L;
    

}
