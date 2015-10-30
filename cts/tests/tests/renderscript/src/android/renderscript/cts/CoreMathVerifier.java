/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.renderscript.cts;

import android.util.Log;

public class CoreMathVerifier {
    static {
        System.loadLibrary("coremathtestcpp_jni");
    }

    /* The level of precision we expect out of the half_* functions.  floats (f32) have 23 bits of
     * mantissa and halfs (f16) have 10 bits.  8192 = 2 ^ (23 - 10).
     */
    private static final int HALF_PRECISION = 8192;
    // The level of precision we expect out of the fast_* functions.
    private static final int FAST_PRECISION = 8192;
    // The level of precision we expect out of the native_* functions.
    private static final int NATIVE_PRECISION = 8192;

    // Static classes used to return multiple values from a few JNI functions.
    static public class FrexpResult {
        public float significand;
        public int exponent;
    }

    static public class LgammaResult {
        public float lgamma;
        public int gammaSign;
    }

    static public class RemquoResult {
        public float remainder;
        public int quotient;
    }

    /* We're calling into native:
     * - not all functions are available in Java, notably gamma and erf,
     * - Java lacks float version of these functions, so we can compare implementations with
     *   similar constraints, and
     * - handling unsigned integers, especially longs, is painful and error prone in Java.
     */
    static native float acos(float x);
    static native float acosh(float x);
    static native float asin(float x);
    static native float asinh(float x);
    static native float atan(float x);
    static native float atan2(float x, float y);
    static native float atanh(float x);
    static native float cbrt(float x);
    static native float ceil(float x);
    static native float cos(float x);
    static native float cosh(float x);
    static native float erf(float x);
    static native float erfc(float x);
    static native float exp(float x);
    static native float exp10(float x);
    static native float exp2(float x);
    static native float expm1(float x);
    static native float floor(float x);
    static native FrexpResult frexp(float x);
    static native float hypot(float x, float y);
    static native int ilogb(float x);
    static native float ldexp(float x, int exp);
    static native float lgamma(float x);
    static native LgammaResult lgamma2(float x);
    static native float log(float x);
    static native float logb(float x);
    static native float log10(float x);
    static native float log1p(float x);
    static native float log2(float x);
    static native byte maxI8(byte x, byte y);
    static native byte maxU8(byte x, byte y);
    static native short maxI16(short x, short y);
    static native short maxU16(short x, short y);
    static native int maxI32(int x, int y);
    static native int maxU32(int x, int y);
    static native long maxI64(long x, long y);
    static native long maxU64(long x, long y);
    static native byte minI8(byte x, byte y);
    static native byte minU8(byte x, byte y);
    static native short minI16(short x, short y);
    static native short minU16(short x, short y);
    static native int minI32(int x, int y);
    static native int minU32(int x, int y);
    static native long minI64(long x, long y);
    static native long minU64(long x, long y);
    static native float pow(float x, float y);
    static native RemquoResult remquo(float numerator, float denominator);
    static native float rint(float x);
    static native float round(float x);
    static native float sin(float x);
    static native float sinh(float x);
    static native float sqrt(float x);
    static native float tan(float x);
    static native float tanh(float x);
    static native float tgamma(float x);
    static native float trunc(float x);

    static native byte   convertCharToChar(byte x);
    static native byte   convertCharToUchar(byte x);
    static native short  convertCharToShort(byte x);
    static native short  convertCharToUshort(byte x);
    static native int    convertCharToInt(byte x);
    static native int    convertCharToUint(byte x);
    static native long   convertCharToLong(byte x);
    static native long   convertCharToUlong(byte x);
    static native float  convertCharToFloat(byte x);
    static native double convertCharToDouble(byte x);

    static native byte   convertUcharToChar(byte x);
    static native byte   convertUcharToUchar(byte x);
    static native short  convertUcharToShort(byte x);
    static native short  convertUcharToUshort(byte x);
    static native int    convertUcharToInt(byte x);
    static native int    convertUcharToUint(byte x);
    static native long   convertUcharToLong(byte x);
    static native long   convertUcharToUlong(byte x);
    static native float  convertUcharToFloat(byte x);
    static native double convertUcharToDouble(byte x);

    static native byte   convertShortToChar(short x);
    static native byte   convertShortToUchar(short x);
    static native short  convertShortToShort(short x);
    static native short  convertShortToUshort(short x);
    static native int    convertShortToInt(short x);
    static native int    convertShortToUint(short x);
    static native long   convertShortToLong(short x);
    static native long   convertShortToUlong(short x);
    static native float  convertShortToFloat(short x);
    static native double convertShortToDouble(short x);

    static native byte   convertUshortToChar(short x);
    static native byte   convertUshortToUchar(short x);
    static native short  convertUshortToShort(short x);
    static native short  convertUshortToUshort(short x);
    static native int    convertUshortToInt(short x);
    static native int    convertUshortToUint(short x);
    static native long   convertUshortToLong(short x);
    static native long   convertUshortToUlong(short x);
    static native float  convertUshortToFloat(short x);
    static native double convertUshortToDouble(short x);

    static native byte   convertIntToChar(int x);
    static native byte   convertIntToUchar(int x);
    static native short  convertIntToShort(int x);
    static native short  convertIntToUshort(int x);
    static native int    convertIntToInt(int x);
    static native int    convertIntToUint(int x);
    static native long   convertIntToLong(int x);
    static native long   convertIntToUlong(int x);
    static native float  convertIntToFloat(int x);
    static native double convertIntToDouble(int x);

    static native byte   convertUintToChar(int x);
    static native byte   convertUintToUchar(int x);
    static native short  convertUintToShort(int x);
    static native short  convertUintToUshort(int x);
    static native int    convertUintToInt(int x);
    static native int    convertUintToUint(int x);
    static native long   convertUintToLong(int x);
    static native long   convertUintToUlong(int x);
    static native float  convertUintToFloat(int x);
    static native double convertUintToDouble(int x);

    static native byte   convertLongToChar(long x);
    static native byte   convertLongToUchar(long x);
    static native short  convertLongToShort(long x);
    static native short  convertLongToUshort(long x);
    static native int    convertLongToInt(long x);
    static native int    convertLongToUint(long x);
    static native long   convertLongToLong(long x);
    static native long   convertLongToUlong(long x);
    static native float  convertLongToFloat(long x);
    static native double convertLongToDouble(long x);

    static native byte   convertUlongToChar(long x);
    static native byte   convertUlongToUchar(long x);
    static native short  convertUlongToShort(long x);
    static native short  convertUlongToUshort(long x);
    static native int    convertUlongToInt(long x);
    static native int    convertUlongToUint(long x);
    static native long   convertUlongToLong(long x);
    static native long   convertUlongToUlong(long x);
    static native float  convertUlongToFloat(long x);
    static native double convertUlongToDouble(long x);

    static native byte   convertFloatToChar(float x);
    static native byte   convertFloatToUchar(float x);
    static native short  convertFloatToShort(float x);
    static native short  convertFloatToUshort(float x);
    static native int    convertFloatToInt(float x);
    static native int    convertFloatToUint(float x);
    static native long   convertFloatToLong(float x);
    static native long   convertFloatToUlong(float x);
    static native float  convertFloatToFloat(float x);
    static native double convertFloatToDouble(float x);

    static native byte   convertDoubleToChar(double x);
    static native byte   convertDoubleToUchar(double x);
    static native short  convertDoubleToShort(double x);
    static native short  convertDoubleToUshort(double x);
    static native int    convertDoubleToInt(double x);
    static native int    convertDoubleToUint(double x);
    static native long   convertDoubleToLong(double x);
    static native long   convertDoubleToUlong(double x);
    static native float  convertDoubleToFloat(double x);
    static native double convertDoubleToDouble(double x);

    static private Target.Floaty pi32(Target t) {
        return t.new32((float) Math.PI);
    }

    static private Target.Floaty any32(Target t) {
        return t.new32(Float.NEGATIVE_INFINITY, Float.NaN, Float.POSITIVE_INFINITY);
    }

    static private Target.Floaty acos(float f, Target t) {
        Target.Floaty in = t.new32(f);
        return t.new32(
            acos(in.mid32()),
            acos(in.min32()),
            acos(in.max32()));
    }

    static private Target.Floaty acosh(float f, Target t) {
        Target.Floaty in = t.new32(f);
        return t.new32(
            acosh(in.mid32()),
            acosh(in.min32()),
            acosh(in.max32()));
    }

    static private Target.Floaty acospi(float f, Target t) {
        return t.divide(acos(f, t), pi32(t));
    }

    static private Target.Floaty asin(float f, Target t) {
        Target.Floaty in = t.new32(f);
        return t.new32(
            asin(in.mid32()),
            asin(in.min32()),
            asin(in.max32()));
    }

    static private Target.Floaty asinh(float f, Target t) {
        Target.Floaty in = t.new32(f);
        return t.new32(
            asinh(in.mid32()),
            asinh(in.min32()),
            asinh(in.max32()));
    }

    static private Target.Floaty asinpi(float f, Target t) {
        return t.divide(asin(f, t), pi32(t));
    }

    static private Target.Floaty atan(float f, Target t) {
        Target.Floaty in = t.new32(f);
        return t.new32(
            atan(in.mid32()),
            atan(in.min32()),
            atan(in.max32()));
    }

    static private Target.Floaty atanh(float f, Target t) {
        Target.Floaty in = t.new32(f);
        return t.new32(
            atanh(in.mid32()),
            atanh(in.min32()),
            atanh(in.max32()));
    }

    static private Target.Floaty atanpi(float f, Target t) {
        return t.divide(atan(f, t), pi32(t));
    }

    static private Target.Floaty atan2(float y, float x, Target t) {
        Target.Floaty numerator = t.new32(y);
        Target.Floaty denominator = t.new32(x);
        return t.new32(
            atan2(numerator.mid32(), denominator.mid32()),
            atan2(numerator.min32(), denominator.min32()),
            atan2(numerator.min32(), denominator.max32()),
            atan2(numerator.max32(), denominator.min32()),
            atan2(numerator.max32(), denominator.max32()));
    }

    static private Target.Floaty atan2pi(float y, float x, Target t) {
        return t.divide(atan2(y, x, t), pi32(t));
    }

    static private Target.Floaty cbrt(float f, Target t) {
        Target.Floaty in = t.new32(f);
        return t.new32(
            cbrt(in.mid32()),
            cbrt(in.min32()),
            cbrt(in.max32()));
    }

    static private Target.Floaty cos(float f, Target t) {
        Target.Floaty in = t.new32(f);
        return t.new32(
            cos(in.mid32()),
            cos(in.min32()),
            cos(in.max32()));
    }

    static private Target.Floaty cosh(float f, Target t) {
        Target.Floaty in = t.new32(f);
        return t.new32(
            cosh(in.mid32()),
            cosh(in.min32()),
            cosh(in.max32()));
    }

    static private Target.Floaty cospi(float f, Target t) {
        Target.Floaty in = t.multiply(t.new32(f), pi32(t));
        return t.new32(
            cos(in.mid32()),
            cos(in.min32()),
            cos(in.max32()));
    }

    // Computes the cross product of two 3D vectors.
    static private void cross(float[] v1, float[] v2, Target.Floaty[] out, Target t) {
        Target.Floaty a12 = t.multiply(t.new32(v1[1]), t.new32(v2[2]));
        Target.Floaty a21 = t.multiply(t.new32(v1[2]), t.new32(v2[1]));
        out[0] = t.subtract(a12, a21);
        Target.Floaty a02 = t.multiply(t.new32(v1[0]), t.new32(v2[2]));
        Target.Floaty a20 = t.multiply(t.new32(v1[2]), t.new32(v2[0]));
        out[1] = t.subtract(a20, a02);
        Target.Floaty a01 = t.multiply(t.new32(v1[0]), t.new32(v2[1]));
        Target.Floaty a10 = t.multiply(t.new32(v1[1]), t.new32(v2[0]));
        out[2] = t.subtract(a01, a10);
        if (out.length == 4) {
            out[3] = t.new32(0.f);
        }
    }

    // Returns the distance between two points in n-dimensional space.
    static private Target.Floaty distance(float[] point1, float[] point2, Target t) {
        Target.Floaty sum = t.new32(0.f);
        for (int i = 0; i < point1.length; i++) {
            Target.Floaty diff = t.subtract(t.new32(point1[i]), t.new32(point2[i]));
            sum = t.add(sum, t.multiply(diff, diff));
        }
        Target.Floaty d = t.sqrt(sum);
        return d;
    }

    static private Target.Floaty exp(float f, Target t) {
        Target.Floaty in = t.new32(f);
        return t.new32(
            exp(in.mid32()),
            exp(in.min32()),
            exp(in.max32()));
    }

    static private Target.Floaty exp10(float f, Target t) {
        Target.Floaty in = t.new32(f);
        return t.new32(
            exp10(in.mid32()),
            exp10(in.min32()),
            exp10(in.max32()));
    }

    static private Target.Floaty exp2(float f, Target t) {
        Target.Floaty in = t.new32(f);
        return t.new32(
            exp2(in.mid32()),
            exp2(in.min32()),
            exp2(in.max32()));
    }

    static private Target.Floaty expm1(float f, Target t) {
        Target.Floaty in = t.new32(f);
        return t.new32(
            expm1(in.mid32()),
            expm1(in.min32()),
            expm1(in.max32()));
    }

    static private Target.Floaty hypot(float x, float y, Target t) {
        Target.Floaty inX = t.new32(x);
        Target.Floaty inY = t.new32(y);
        return t.new32(
            hypot(inX.mid32(), inY.mid32()),
            hypot(inX.min32(), inY.min32()),
            hypot(inX.min32(), inY.max32()),
            hypot(inX.max32(), inY.min32()),
            hypot(inX.max32(), inY.max32()));
    }

    // Returns the length of the n-dimensional vector.
    static private Target.Floaty length(float[] array, Target t) {
        Target.Floaty sum = t.new32(0.f);
        for (int i = 0; i < array.length; i++) {
            Target.Floaty f = t.new32(array[i]);
            sum = t.add(sum, t.multiply(f, f));
        }
        Target.Floaty l = t.sqrt(sum);
        return l;
    }

    static private Target.Floaty log(float f, Target t) {
        Target.Floaty in = t.new32(f);
        return t.new32(
            log(in.mid32()),
            log(in.min32()),
            log(in.max32()));
    }

    static private Target.Floaty log10(float f, Target t) {
        Target.Floaty in = t.new32(f);
        return t.new32(
            log10(in.mid32()),
            log10(in.min32()),
            log10(in.max32()));
    }

    static private Target.Floaty log1p(float f, Target t) {
        Target.Floaty in = t.new32(f);
        return t.new32(
            log1p(in.mid32()),
            log1p(in.min32()),
            log1p(in.max32()));
    }

    static private Target.Floaty log2(float f, Target t) {
        Target.Floaty in = t.new32(f);
        return t.new32(
            log2(in.mid32()),
            log2(in.min32()),
            log2(in.max32()));
    }

    // Normalizes the n-dimensional vector, i.e. makes it length 1.
    static private void normalize(float[] in, Target.Floaty[] out, Target t) {
        Target.Floaty l = length(in, t);
        boolean isZero = l.get32() == 0.f;
        for (int i = 0; i < in.length; i++) {
            out[i] = t.new32(in[i]);
            if (!isZero) {
                out[i] = t.divide(out[i], l);
            }
        }
    }

    static private Target.Floaty powr(float x, float y, Target t) {
        Target.Floaty base = t.new32(x);
        Target.Floaty exponent = t.new32(y);
        return t.new32(
            pow(base.mid32(), exponent.mid32()),
            pow(base.min32(), exponent.min32()),
            pow(base.min32(), exponent.max32()),
            pow(base.max32(), exponent.min32()),
            pow(base.max32(), exponent.max32()));
    }

    static private Target.Floaty recip(float f, Target t) {
        Target.Floaty in = t.new32(f);
        return t.divide(t.new32(1.f), in);
    }

    static private Target.Floaty rootn(float inV, int inN, Target t) {
        /* Rootn of a negative number should be possible only if the number
         * is odd.  In cases where the int is very large, our approach will
         * lose whether the int is odd, and we'll get a NaN for weird cases
         * like rootn(-3.95, 818181881), which should return 1.  We handle the
         * case by handling the sign ourselves.  We use copysign to handle the
         * negative zero case.
         */
        float value;
        if ((inN & 0x1) == 0x1) {
            value = Math.copySign(pow(Math.abs(inV), 1.f / inN),
                    inV);
        } else {
            value = pow(inV, 1.f / inN);
        }
        if (inN == 0) {
            return t.new32(value, Float.NaN);
        } else {
            return t.new32(value);
        }
    }

    static private Target.Floaty rsqrt(float f, Target t) {
        Target.Floaty in = t.new32(f);
        return t.divide(t.new32(1.f), t.sqrt(in));
    }

    static private Target.Floaty sin(float f, Target t) {
        Target.Floaty in = t.new32(f);
        return t.new32(
            sin(in.mid32()),
            sin(in.min32()),
            sin(in.max32()));
    }

    static private Target.Floaty sinh(float f, Target t) {
        Target.Floaty in = t.new32(f);
        return t.new32(
            sinh(in.mid32()),
            sinh(in.min32()),
            sinh(in.max32()));
    }

    static private Target.Floaty sinpi(float f, Target t) {
        Target.Floaty in = t.multiply(t.new32(f), pi32(t));
        return t.new32(
            sin(in.mid32()),
            sin(in.min32()),
            sin(in.max32()));
    }

    static private Target.Floaty sqrt(float f, Target t) {
        Target.Floaty in = t.new32(f);
        return t.sqrt(in);
    }

    static private Target.Floaty tan(float f, Target t) {
        Target.Floaty in = t.new32(f);
        float min = tan(in.min32());
        float max = tan(in.max32());
        /* If the tan of the min is greater than that of the max,
         * we spanned a discontinuity.
         */
        if (min > max) {
            return any32(t);
        } else {
            return t.new32(tan(f), min, max);
        }
    }

    static private Target.Floaty tanh(float f, Target t) {
        Target.Floaty in = t.new32(f);
        return t.new32(
            tanh(in.mid32()),
            tanh(in.min32()),
            tanh(in.max32()));
    }

    static private Target.Floaty tanpi(float f, Target t) {
        Target.Floaty in = t.multiply(t.new32(f), pi32(t));
        float min = tan(in.min32());
        float max = tan(in.max32());
        /* If the tan of the min is greater than that of the max,
         * we spanned a discontinuity.
         */
        if (min > max) {
            return any32(t);
        } else {
            return t.new32(tan(in.mid32()), min, max);
        }
    }

    static public void computeAbs(TestAbs.ArgumentsCharUchar args) {
        args.out = (byte)Math.abs(args.inV);
    }

    static public void computeAbs(TestAbs.ArgumentsShortUshort args) {
        args.out = (short)Math.abs(args.inV);
    }

    static public void computeAbs(TestAbs.ArgumentsIntUint args) {
        args.out = Math.abs(args.inV);
    }

    static public void computeAcos(TestAcos.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(4, 128, false);
        args.out = acos(args.inV, t);
    }

    static public void computeAcosh(TestAcosh.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(4, 128, false);
        args.out = acosh(args.inV, t);
    }

    static public void computeAcospi(TestAcospi.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(5, 128, false);
        args.out = acospi(args.inV, t);
    }

    static public void computeAsin(TestAsin.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(4, 128, false);
        args.out = asin(args.inV, t);
    }

    static public void computeAsinh(TestAsinh.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(4, 128, false);
        args.out = asinh(args.inV, t);
    }

    static public void computeAsinpi(TestAsinpi.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(5, 128, false);
        args.out = asinpi(args.inV, t);
    }

    static public void computeAtan(TestAtan.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(5, 128, false);
        args.out = atan(args.inV, t);
    }

    static public void computeAtanh(TestAtanh.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(5, 128, false);
        args.out = atanh(args.inV, t);
    }

    static public void computeAtanpi(TestAtanpi.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(5, 128, false);
        args.out = atanpi(args.inV, t);
    }

    static public void computeAtan2(TestAtan2.ArgumentsFloatFloatFloat args, Target t) {
        t.setPrecision(6, 128, false);
        args.out = atan2(args.inNumerator, args.inDenominator, t);
    }

    static public void computeAtan2pi(TestAtan2pi.ArgumentsFloatFloatFloat args, Target t) {
        t.setPrecision(6, 128, false);
        args.out = atan2pi(args.inNumerator, args.inDenominator, t);
    }

    static public void computeCbrt(TestCbrt.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(2, 128, false);
        args.out = cbrt(args.inV, t);
    }

    static public void computeCeil(TestCeil.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(0, 1, false);
        Target.Floaty in = t.new32(args.inV);
        args.out = t.new32(
            ceil(in.mid32()),
            ceil(in.min32()),
            ceil(in.max32()));
    }

    static public void computeClamp(TestClamp.ArgumentsCharCharCharChar args) {
        args.out = minI8(args.inMaxValue, maxI8(args.inValue, args.inMinValue));
    }

    static public void computeClamp(TestClamp.ArgumentsUcharUcharUcharUchar args) {
        args.out = minU8(args.inMaxValue, maxU8(args.inValue, args.inMinValue));
    }

    static public void computeClamp(TestClamp.ArgumentsShortShortShortShort args) {
        args.out = minI16(args.inMaxValue, maxI16(args.inValue, args.inMinValue));
    }

    static public void computeClamp(TestClamp.ArgumentsUshortUshortUshortUshort args) {
        args.out = minU16(args.inMaxValue, maxU16(args.inValue, args.inMinValue));
    }

    static public void computeClamp(TestClamp.ArgumentsIntIntIntInt args) {
        args.out = minI32(args.inMaxValue, maxI32(args.inValue, args.inMinValue));
    }

    static public void computeClamp(TestClamp.ArgumentsUintUintUintUint args) {
        args.out = minU32(args.inMaxValue, maxU32(args.inValue, args.inMinValue));
    }

    static public void computeClamp(TestClamp.ArgumentsFloatFloatFloatFloat args, Target t) {
        t.setPrecision(0, 0, false);
        args.out = t.new32(Math.min(args.inMaxValue,
                        Math.max(args.inValue, args.inMinValue)));
    }

    static public void computeClamp(TestClamp.ArgumentsLongLongLongLong args) {
        args.out = minI64(args.inMaxValue, maxI64(args.inValue, args.inMinValue));
    }

    static public void computeClamp(TestClamp.ArgumentsUlongUlongUlongUlong args) {
        args.out = minU64(args.inMaxValue, maxU64(args.inValue, args.inMinValue));
    }

    static public void computeClz(TestClz.ArgumentsCharChar args) {
        int x = args.inValue;
        args.out = (byte) (Integer.numberOfLeadingZeros(x & 0xff) - 24);
    }

    static public void computeClz(TestClz.ArgumentsUcharUchar args) {
        int x = args.inValue;
        args.out = (byte) (Integer.numberOfLeadingZeros(x & 0xff) - 24);
    }

    static public void computeClz(TestClz.ArgumentsShortShort args) {
        args.out = (short) (Integer.numberOfLeadingZeros(args.inValue & 0xffff) - 16);
    }

    static public void computeClz(TestClz.ArgumentsUshortUshort args) {
        args.out = (short) (Integer.numberOfLeadingZeros(args.inValue & 0xffff) - 16);
    }

    static public void computeClz(TestClz.ArgumentsIntInt args) {
        args.out = (int) Integer.numberOfLeadingZeros(args.inValue);
    }

    static public void computeClz(TestClz.ArgumentsUintUint args) {
        args.out = (int) Integer.numberOfLeadingZeros(args.inValue);
    }


    static public void computeConvert(TestConvert.ArgumentsCharChar args) {
        args.out = convertCharToChar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsCharUchar args) {
        args.out = convertCharToUchar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsCharShort args) {
        args.out = convertCharToShort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsCharUshort args) {
        args.out = convertCharToUshort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsCharInt args) {
        args.out = convertCharToInt(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsCharUint args) {
        args.out = convertCharToUint(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsCharLong args) {
        args.out = convertCharToLong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsCharUlong args) {
        args.out = convertCharToUlong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsCharFloat args, Target t) {
        t.setPrecision(0, 0, false);
        args.out = t.new32(convertCharToFloat(args.inV));
    }
    static public void computeConvert(TestConvert.ArgumentsCharDouble args, Target t) {
        t.setPrecision(0, 0, false);
        args.out = t.new64(convertCharToDouble(args.inV));
    }

    static public void computeConvert(TestConvert.ArgumentsUcharChar args) {
        args.out = convertUcharToChar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUcharUchar args) {
        args.out = convertUcharToUchar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUcharShort args) {
        args.out = convertUcharToShort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUcharUshort args) {
        args.out = convertUcharToUshort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUcharInt args) {
        args.out = convertUcharToInt(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUcharUint args) {
        args.out = convertUcharToUint(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUcharLong args) {
        args.out = convertUcharToLong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUcharUlong args) {
        args.out = convertUcharToUlong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUcharFloat args, Target t) {
        t.setPrecision(0, 0, false);
        args.out = t.new32(convertUcharToFloat(args.inV));
    }
    static public void computeConvert(TestConvert.ArgumentsUcharDouble args, Target t) {
        t.setPrecision(0, 0, false);
        args.out = t.new64(convertUcharToDouble(args.inV));
    }

    static public void computeConvert(TestConvert.ArgumentsShortChar args) {
        args.out = convertShortToChar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsShortUchar args) {
        args.out = convertShortToUchar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsShortShort args) {
        args.out = convertShortToShort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsShortUshort args) {
        args.out = convertShortToUshort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsShortInt args) {
        args.out = convertShortToInt(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsShortUint args) {
        args.out = convertShortToUint(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsShortLong args) {
        args.out = convertShortToLong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsShortUlong args) {
        args.out = convertShortToUlong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsShortFloat args, Target t) {
        t.setPrecision(0, 0, false);
        args.out = t.new32(convertShortToFloat(args.inV));
    }
    static public void computeConvert(TestConvert.ArgumentsShortDouble args, Target t) {
        t.setPrecision(0, 0, false);
        args.out = t.new64(convertShortToDouble(args.inV));
    }

    static public void computeConvert(TestConvert.ArgumentsUshortChar args) {
        args.out = convertUshortToChar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUshortUchar args) {
        args.out = convertUshortToUchar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUshortShort args) {
        args.out = convertUshortToShort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUshortUshort args) {
        args.out = convertUshortToUshort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUshortInt args) {
        args.out = convertUshortToInt(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUshortUint args) {
        args.out = convertUshortToUint(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUshortLong args) {
        args.out = convertUshortToLong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUshortUlong args) {
        args.out = convertUshortToUlong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUshortFloat args, Target t) {
        t.setPrecision(0, 0, false);
        args.out = t.new32(convertUshortToFloat(args.inV));
    }
    static public void computeConvert(TestConvert.ArgumentsUshortDouble args, Target t) {
        t.setPrecision(0, 0, false);
        args.out = t.new64(convertUshortToDouble(args.inV));
    }

    static public void computeConvert(TestConvert.ArgumentsIntChar args) {
        args.out = convertIntToChar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsIntUchar args) {
        args.out = convertIntToUchar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsIntShort args) {
        args.out = convertIntToShort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsIntUshort args) {
        args.out = convertIntToUshort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsIntInt args) {
        args.out = convertIntToInt(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsIntUint args) {
        args.out = convertIntToUint(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsIntLong args) {
        args.out = convertIntToLong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsIntUlong args) {
        args.out = convertIntToUlong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsIntFloat args, Target t) {
        t.setPrecision(1, 1, false);
        args.out = t.new32(convertIntToFloat(args.inV));
    }
    static public void computeConvert(TestConvert.ArgumentsIntDouble args, Target t) {
        t.setPrecision(0, 0, false);
        args.out = t.new64(convertIntToDouble(args.inV));
    }

    static public void computeConvert(TestConvert.ArgumentsUintChar args) {
        args.out = convertUintToChar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUintUchar args) {
        args.out = convertUintToUchar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUintShort args) {
        args.out = convertUintToShort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUintUshort args) {
        args.out = convertUintToUshort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUintInt args) {
        args.out = convertUintToInt(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUintUint args) {
        args.out = convertUintToUint(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUintLong args) {
        args.out = convertUintToLong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUintUlong args) {
        args.out = convertUintToUlong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUintFloat args, Target t) {
        t.setPrecision(1, 1, false);
        args.out = t.new32(convertUintToFloat(args.inV));
    }
    static public void computeConvert(TestConvert.ArgumentsUintDouble args, Target t) {
        t.setPrecision(0, 0, false);
        args.out = t.new64(convertUintToDouble(args.inV));
    }

    static public void computeConvert(TestConvert.ArgumentsLongChar args) {
        args.out = convertLongToChar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsLongUchar args) {
        args.out = convertLongToUchar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsLongShort args) {
        args.out = convertLongToShort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsLongUshort args) {
        args.out = convertLongToUshort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsLongInt args) {
        args.out = convertLongToInt(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsLongUint args) {
        args.out = convertLongToUint(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsLongLong args) {
        args.out = convertLongToLong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsLongUlong args) {
        args.out = convertLongToUlong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsLongFloat args, Target t) {
        t.setPrecision(1, 1, false);
        args.out = t.new32(convertLongToFloat(args.inV));
    }
    static public void computeConvert(TestConvert.ArgumentsLongDouble args, Target t) {
        t.setPrecision(1, 1, false);
        args.out = t.new64(convertLongToDouble(args.inV));
    }

    static public void computeConvert(TestConvert.ArgumentsUlongChar args) {
        args.out = convertUlongToChar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUlongUchar args) {
        args.out = convertUlongToUchar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUlongShort args) {
        args.out = convertUlongToShort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUlongUshort args) {
        args.out = convertUlongToUshort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUlongInt args) {
        args.out = convertUlongToInt(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUlongUint args) {
        args.out = convertUlongToUint(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUlongLong args) {
        args.out = convertUlongToLong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUlongUlong args) {
        args.out = convertUlongToUlong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUlongFloat args, Target t) {
        t.setPrecision(1, 1, false);
        args.out = t.new32(convertUlongToFloat(args.inV));
    }
    static public void computeConvert(TestConvert.ArgumentsUlongDouble args, Target t) {
        t.setPrecision(1, 1, false);
        args.out = t.new64(convertUlongToDouble(args.inV));
    }

    static public void computeConvert(TestConvert.ArgumentsFloatChar args) {
        args.out = convertFloatToChar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsFloatUchar args) {
        args.out = convertFloatToUchar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsFloatShort args) {
        args.out = convertFloatToShort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsFloatUshort args) {
        args.out = convertFloatToUshort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsFloatInt args) {
        args.out = convertFloatToInt(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsFloatUint args) {
        args.out = convertFloatToUint(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsFloatLong args) {
        args.out = convertFloatToLong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsFloatUlong args) {
        args.out = convertFloatToUlong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(0, 0, false);
        args.out = t.new32(convertFloatToFloat(args.inV));
    }
    static public void computeConvert(TestConvert.ArgumentsFloatDouble args, Target t) {
        t.setPrecision(0, 0, false);
        args.out = t.new64(convertFloatToDouble(args.inV));
    }

    static public void computeConvert(TestConvert.ArgumentsDoubleChar args) {
        args.out = convertDoubleToChar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsDoubleUchar args) {
        args.out = convertDoubleToUchar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsDoubleShort args) {
        args.out = convertDoubleToShort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsDoubleUshort args) {
        args.out = convertDoubleToUshort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsDoubleInt args) {
        args.out = convertDoubleToInt(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsDoubleUint args) {
        args.out = convertDoubleToUint(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsDoubleLong args) {
        args.out = convertDoubleToLong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsDoubleUlong args) {
        args.out = convertDoubleToUlong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsDoubleFloat args, Target t) {
        t.setPrecision(1, 1, false);
        args.out = t.new32(convertDoubleToFloat(args.inV));
    }
    static public void computeConvert(TestConvert.ArgumentsDoubleDouble args, Target t) {
        t.setPrecision(0, 0, false);
        args.out = t.new64(convertDoubleToDouble(args.inV));
    }

    static public void computeCopysign(TestCopysign.ArgumentsFloatFloatFloat args, Target t) {
        t.setPrecision(0, 0, false);
        args.out = t.new32(Math.copySign(args.inMagnitudeValue, args.inSignValue));
    }

    static public void computeCos(TestCos.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(4, 128, false);
        args.out = cos(args.inV, t);
    }

    static public void computeCosh(TestCosh.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(4, 128, false);
        args.out = cosh(args.inV, t);
    }

    static public void computeCospi(TestCospi.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(4, 128, false);
        args.out = cospi(args.inV, t);
    }

    static public void computeCross(TestCross.ArgumentsFloatNFloatNFloatN args, Target t) {
        t.setPrecision(1, 4, false);
        cross(args.inLeftVector, args.inRightVector, args.out, t);
    }

    static public void computeDegrees(TestDegrees.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(3, 3, false);
        Target.Floaty in = t.new32(args.inV);
        Target.Floaty k = t.new32((float)(180.0 / Math.PI));
        args.out = t.multiply(in, k);
    }

    static public void computeDistance(TestDistance.ArgumentsFloatFloatFloat args, Target t) {
        t.setPrecision(1, 1, false);
        args.out = distance(new float[] {args.inLeftVector}, new float[] {args.inRightVector}, t);
    }

    static public void computeDistance(TestDistance.ArgumentsFloatNFloatNFloat args, Target t) {
        t.setPrecision(1, 1, false);
        args.out = distance(args.inLeftVector, args.inRightVector, t);
    }

    static public void computeDot(TestDot.ArgumentsFloatFloatFloat args, Target t) {
        t.setPrecision(1, 4, false);
        Target.Floaty a = t.new32(args.inLeftVector);
        Target.Floaty b = t.new32(args.inRightVector);
        args.out = t.multiply(a, b);
    }

    static public void computeDot(TestDot.ArgumentsFloatNFloatNFloat args, Target t) {
        t.setPrecision(1, 4, false);
        Target.Floaty sum = t.new32(0.f);
        for (int i = 0; i < args.inLeftVector.length; i++) {
            Target.Floaty a = t.new32(args.inLeftVector[i]);
            Target.Floaty b = t.new32(args.inRightVector[i]);
            sum = t.add(sum, t.multiply(a, b));
        }
        args.out = sum;
    }

    static public void computeErf(TestErf.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(16, 128, false);
        Target.Floaty in = t.new32(args.inV);
        args.out = t.new32(
            erf(args.inV),
            erf(in.min32()),
            erf(in.max32()));
    }

    static public void computeErfc(TestErfc.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(16, 128, false);
        Target.Floaty in = t.new32(args.inV);
        args.out = t.new32(
            erfc(args.inV),
            erfc(in.min32()),
            erfc(in.max32()));
    }

    static public void computeExp(TestExp.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(3, 16, false);
        args.out = exp(args.inV, t);
    }

    static public void computeExp10(TestExp10.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(3, 32, false);
        args.out = exp10(args.inV, t);
    }

    static public void computeExp2(TestExp2.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(3, 16, false);
        args.out = exp2(args.inV, t);
    }

    static public void computeExpm1(TestExpm1.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(3, 16, false);
        args.out = expm1(args.inV, t);
    }

    static public void computeFabs(TestFabs.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(0, 0, false);
        Target.Floaty in = t.new32(args.inV);
        args.out = t.new32(
            Math.abs(args.inV),
            Math.abs(in.min32()),
            Math.abs(in.max32()));
    }

    static public void computeFastDistance(TestFastDistance.ArgumentsFloatFloatFloat args, Target t) {
        t.setPrecision(FAST_PRECISION, FAST_PRECISION, false);
        args.out = distance(new float[] {args.inLeftVector}, new float[] {args.inRightVector}, t);
    }

    static public void computeFastDistance(TestFastDistance.ArgumentsFloatNFloatNFloat args, Target t) {
        t.setPrecision(FAST_PRECISION, FAST_PRECISION, false);
        args.out = distance(args.inLeftVector, args.inRightVector, t);
    }

    static public void computeFastLength(TestFastLength.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(FAST_PRECISION, FAST_PRECISION, false);
        args.out = length(new float[] {args.inV}, t);
    }

    static public void computeFastLength(TestFastLength.ArgumentsFloatNFloat args, Target t) {
        t.setPrecision(FAST_PRECISION, FAST_PRECISION, false);
        args.out = length(args.inV, t);
    }

    static public void computeFastNormalize(TestFastNormalize.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(FAST_PRECISION, FAST_PRECISION, false);
        Target.Floaty[] out = new Target.Floaty[1];
        normalize(new float[] {args.inV}, out, t);
        args.out = out[0];
    }

    static public void computeFastNormalize(TestFastNormalize.ArgumentsFloatNFloatN args, Target t) {
        t.setPrecision(FAST_PRECISION, FAST_PRECISION, false);
        normalize(args.inV, args.out, t);
    }

    static public void computeFdim(TestFdim.ArgumentsFloatFloatFloat args, Target t) {
        t.setPrecision(1, 1, false);
        Target.Floaty inA = t.new32(args.inA);
        Target.Floaty inB = t.new32(args.inB);
        Target.Floaty r = t.subtract(inA, inB);
        args.out = t.new32(
            Math.max(0.f, r.mid32()),
            Math.max(0.f, r.min32()),
            Math.max(0.f, r.max32()));
    }

    static public void computeFloor(TestFloor.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(0, 0, false);
        Target.Floaty in = t.new32(args.inV);
        args.out = t.new32(
            floor(args.inV),
            floor(in.min32()),
            floor(in.max32()));
    }

    static public void computeFma(TestFma.ArgumentsFloatFloatFloatFloat args, Target t) {
        t.setPrecision(1, 1, false);
        Target.Floaty ab = t.multiply(t.new32(args.inMultiplicand1), t.new32(args.inMultiplicand2));
        args.out = t.add(ab, t.new32(args.inOffset));
    }

    static public void computeFmax(TestFmax.ArgumentsFloatFloatFloat args, Target t) {
        t.setPrecision(0, 0, false);
        Target.Floaty a = t.new32(args.inA);
        Target.Floaty b = t.new32(args.inB);
        args.out = t.new32(
            Math.max(args.inA, args.inB),
            Math.max(a.min32(), b.min32()),
            Math.max(a.min32(), b.max32()),
            Math.max(a.max32(), b.min32()),
            Math.max(a.max32(), b.max32()));
    }

    static public void computeFmin(TestFmin.ArgumentsFloatFloatFloat args, Target t) {
        t.setPrecision(0, 0, false);
        Target.Floaty a = t.new32(args.inA);
        Target.Floaty b = t.new32(args.inB);
        args.out = t.new32(
            Math.min(args.inA, args.inB),
            Math.min(a.min32(), b.min32()),
            Math.min(a.min32(), b.max32()),
            Math.min(a.max32(), b.min32()),
            Math.min(a.max32(), b.max32()));
    }

    static public void computeFmod(TestFmod.ArgumentsFloatFloatFloat args, Target t) {
        t.setPrecision(1, 1, false);
        Target.Floaty numerator = t.new32(args.inNumerator);
        Target.Floaty denominator = t.new32(args.inDenominator);
        args.out = t.new32(
            args.inNumerator % args.inDenominator,
            numerator.min32() % denominator.min32(),
            numerator.min32() % denominator.max32(),
            numerator.max32() % denominator.min32(),
            numerator.max32() % denominator.max32());
    }

    static public void computeFract(TestFract.ArgumentsFloatFloatFloat args, Target t) {
        t.setPrecision(1, 1, false);
        float floor = floor(args.inV);
        args.outFloor = t.new32(floor);
        // 0x1.fffffep-1f is 0.999999...
        args.out = t.new32(Math.min(args.inV - floor, 0x1.fffffep-1f));
    }

    static public void computeFract(TestFract.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(1, 1, false);
        float floor = floor(args.inV);
        // 0x1.fffffep-1f is 0.999999...
        args.out = t.new32(Math.min(args.inV - floor, 0x1.fffffep-1f));
    }

    static public void computeFrexp(TestFrexp.ArgumentsFloatIntFloat args, Target t) {
        t.setPrecision(0, 0, false);
        FrexpResult result = frexp(args.inV);
        args.out = t.new32(result.significand);
        args.outExponent = result.exponent;
    }

    static public void computeHalfRecip(TestHalfRecip.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(HALF_PRECISION, HALF_PRECISION, false);
        args.out = recip(args.inV, t);
    }

    static public void computeHalfRsqrt(TestHalfRsqrt.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(HALF_PRECISION, HALF_PRECISION, false);
        args.out = rsqrt(args.inV, t);
    }

    static public void computeHalfSqrt(TestHalfSqrt.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(HALF_PRECISION, HALF_PRECISION, false);
        args.out = sqrt(args.inV, t);
    }

    static public void computeHypot(TestHypot.ArgumentsFloatFloatFloat args, Target t) {
        t.setPrecision(4, 4, false);
        args.out = hypot(args.inA, args.inB, t);
    }

    static public String verifyIlogb(TestIlogb.ArgumentsFloatInt args) {
        // Special case when the input is 0.  We accept two different answers.
        if (args.inV == 0.f) {
            if (args.out != -Integer.MAX_VALUE && args.out != Integer.MIN_VALUE) {
                return "Expected " + Integer.toString(-Integer.MAX_VALUE) + " or " +
                    Integer.toString(Integer.MIN_VALUE);
            }
        } else {
            int result = ilogb(args.inV);
            if (args.out != result) {
                return "Expected " + Integer.toString(result);
            }
        }
        return null;
    }

    static public void computeLdexp(TestLdexp.ArgumentsFloatIntFloat args, Target t) {
        t.setPrecision(1, 1, false);
        Target.Floaty inMantissa = t.new32(args.inMantissa);
        args.out = t.new32(
            ldexp(inMantissa.mid32(), args.inExponent),
            ldexp(inMantissa.min32(), args.inExponent),
            ldexp(inMantissa.max32(), args.inExponent));
    }

    static public void computeLength(TestLength.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(1, 1, false);
        args.out = length(new float[]{args.inV}, t);
    }

    static public void computeLength(TestLength.ArgumentsFloatNFloat args, Target t) {
        t.setPrecision(1, 1, false);
        args.out = length(args.inV, t);
    }

    static public void computeLgamma(TestLgamma.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(16, 128, false);
        Target.Floaty in = t.new32(args.inV);
        args.out = t.new32(
            lgamma(in.mid32()),
            lgamma(in.min32()),
            lgamma(in.max32()));
    }

    /* TODO Until -0 handling is corrected in bionic & associated drivers, we temporarily
     * disable the verification of -0.  We do this with a custom verifier.  Once bionic
     * is fixed, we can restore computeLgamma and remove verifyLgamma.
    static public void computeLgamma(TestLgamma.ArgumentsFloatIntFloat args, Target t) {
        t.setPrecision(16, 128, false);
        Target.Floaty in = t.new32(args.inV);
        LgammaResult result = lgamma2(in.mid32());
        LgammaResult resultMin = lgamma2(in.min32());
        LgammaResult resultMax = lgamma2(in.max32());
        args.out = t.new32(result.lgamma, resultMin.lgamma, resultMax.lgamma);
        args.outY = result.gammaSign;
    }
    */
    static public String verifyLgamma(TestLgamma.ArgumentsFloatIntFloat args, Target t) {
        t.setPrecision(16, 128, false);
        Target.Floaty in = t.new32(args.inV);
        LgammaResult result = lgamma2(in.mid32());
        LgammaResult resultMin = lgamma2(in.min32());
        LgammaResult resultMax = lgamma2(in.max32());
        Target.Floaty expectedOut = t.new32(result.lgamma, resultMin.lgamma, resultMax.lgamma);
        boolean isNegativeZero = args.inV == 0.f && 1.f / args.inV < 0.f;
        /* TODO The current implementation of bionic does not handle the -0.f case correctly.
         * It should set the sign to -1 but sets it to 1.
         */
        if (!expectedOut.couldBe(args.out) ||
            (args.outSignOfGamma != result.gammaSign && !isNegativeZero)) {
            StringBuilder message = new StringBuilder();
            message.append(String.format("Input in %14.8g {%8x}:\n", args.inV, Float.floatToRawIntBits(args.inV)));
            message.append("Expected out: ");
            message.append(expectedOut.toString());
            message.append("\n");
            message.append(String.format("Actual   out: %14.8g {%8x}", args.out, Float.floatToRawIntBits(args.out)));
            message.append(String.format("Expected outSign: %d\n", result.gammaSign));
            message.append(String.format("Actual   outSign: %d\n", args.outSignOfGamma));
            return message.toString();
        }

        return null;
    }

    // TODO The relaxed ulf for the various log are taken from the old tests.
    // They are not consistent.
    static public void computeLog(TestLog.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(3, 16, false);
        args.out = log(args.inV, t);
    }

    static public void computeLog10(TestLog10.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(3, 16, false);
        args.out = log10(args.inV, t);
    }

    static public void computeLog1p(TestLog1p.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(2, 16, false);
        args.out = log1p(args.inV, t);
    }

    static public void computeLog2(TestLog2.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(3, 128, false);
        args.out = log2(args.inV, t);
    }

    static public void computeLogb(TestLogb.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(0, 0, false);
        Target.Floaty in = t.new32(args.inV);
        args.out = t.new32(
            logb(in.mid32()),
            logb(in.min32()),
            logb(in.max32()));
    }

    static public void computeMad(TestMad.ArgumentsFloatFloatFloatFloat args, Target t) {
        t.setPrecision(1, 4, false);
        Target.Floaty ab = t.multiply(t.new32(args.inMultiplicand1), t.new32(args.inMultiplicand2));
        args.out = t.add(ab, t.new32(args.inOffset));
    }

    static public void computeMax(TestMax.ArgumentsCharCharChar args) {
        args.out = maxI8(args.inA, args.inB);
    }

    static public void computeMax(TestMax.ArgumentsUcharUcharUchar args) {
        args.out = maxU8(args.inA, args.inB);
    }

    static public void computeMax(TestMax.ArgumentsShortShortShort args) {
        args.out = maxI16(args.inA, args.inB);
    }

    static public void computeMax(TestMax.ArgumentsUshortUshortUshort args) {
        args.out = maxU16(args.inA, args.inB);
    }

    static public void computeMax(TestMax.ArgumentsIntIntInt args) {
        args.out = maxI32(args.inA, args.inB);
    }

    static public void computeMax(TestMax.ArgumentsUintUintUint args) {
        args.out = maxU32(args.inA, args.inB);
    }

    static public void computeMax(TestMax.ArgumentsLongLongLong args) {
        args.out = maxI64(args.inA, args.inB);
    }

    static public void computeMax(TestMax.ArgumentsUlongUlongUlong args) {
        args.out = maxU64(args.inA, args.inB);
    }

    static public void computeMax(TestMax.ArgumentsFloatFloatFloat args, Target t) {
        t.setPrecision(0, 0, false);
        Target.Floaty a = t.new32(args.inA);
        Target.Floaty b = t.new32(args.inB);
        args.out = t.new32(
            Math.max(a.mid32(), b.mid32()),
            Math.max(a.min32(), b.min32()),
            Math.max(a.min32(), b.max32()),
            Math.max(a.max32(), b.min32()),
            Math.max(a.max32(), b.max32()));
    }

    static public void computeMin(TestMin.ArgumentsCharCharChar args) {
        args.out = minI8(args.inA, args.inB);
    }

    static public void computeMin(TestMin.ArgumentsUcharUcharUchar args) {
        args.out = minU8(args.inA, args.inB);
    }

    static public void computeMin(TestMin.ArgumentsShortShortShort args) {
        args.out = minI16(args.inA, args.inB);
    }

    static public void computeMin(TestMin.ArgumentsUshortUshortUshort args) {
        args.out = minU16(args.inA, args.inB);
    }

    static public void computeMin(TestMin.ArgumentsIntIntInt args) {
        args.out = minI32(args.inA, args.inB);
    }

    static public void computeMin(TestMin.ArgumentsUintUintUint args) {
        args.out = minU32(args.inA, args.inB);
    }

    static public void computeMin(TestMin.ArgumentsLongLongLong args) {
        args.out = minI64(args.inA, args.inB);
    }

    static public void computeMin(TestMin.ArgumentsUlongUlongUlong args) {
        args.out = minU64(args.inA, args.inB);
    }

    static public void computeMin(TestMin.ArgumentsFloatFloatFloat args, Target t) {
        t.setPrecision(0, 0, false);
        args.out = t.new32(Math.min(args.inA, args.inB));
    }

    static public void computeMix(TestMix.ArgumentsFloatFloatFloatFloat args, Target t) {
        t.setPrecision(1, 4, false);
        Target.Floaty start = t.new32(args.inStart);
        Target.Floaty stop = t.new32(args.inStop);
        Target.Floaty diff = t.subtract(stop, start);
        args.out = t.add(start, t.multiply(diff, t.new32(args.inFraction)));
    }

    static public void computeModf(TestModf.ArgumentsFloatFloatFloat args, Target t) {
        t.setPrecision(0, 0, false);
        float ret = (float)(int)args.inV;
        args.outIntegralPart = t.new32(ret);
        args.out = t.new32(args.inV - ret);
    }

    static public void computeNan(TestNan.ArgumentsUintFloat args, Target t) {
        t.setPrecision(0, 0, false);
        // TODO(jeanluc) We're not using the input argument
        args.out = t.new32(Float.NaN);
    }

    static public void computeNativeAcos(TestNativeAcos.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = acos(args.inV, t);
    }

    static public void computeNativeAcosh(TestNativeAcosh.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = acosh(args.inV, t);
    }

    static public void computeNativeAcospi(TestNativeAcospi.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = acospi(args.inV, t);
    }

    static public void computeNativeAsin(TestNativeAsin.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = asin(args.inV, t);
    }

    static public void computeNativeAsinh(TestNativeAsinh.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = asinh(args.inV, t);
    }

    static public void computeNativeAsinpi(TestNativeAsinpi.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = asinpi(args.inV, t);
    }

    static public void computeNativeAtan(TestNativeAtan.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = atan(args.inV, t);
    }

    static public void computeNativeAtanh(TestNativeAtanh.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = atanh(args.inV, t);
    }

    static public void computeNativeAtanpi(TestNativeAtanpi.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = atanpi(args.inV, t);
    }

    static public void computeNativeAtan2(TestNativeAtan2.ArgumentsFloatFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = atan2(args.inNumerator, args.inDenominator, t);
    }

    static public void computeNativeAtan2pi(TestNativeAtan2pi.ArgumentsFloatFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = atan2pi(args.inNumerator, args.inDenominator, t);
    }

    static public void computeNativeCbrt(TestNativeCbrt.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = cbrt(args.inV, t);
    }

    static public void computeNativeCos(TestNativeCos.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = cos(args.inV, t);
    }

    static public void computeNativeCosh(TestNativeCosh.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = cosh(args.inV, t);
    }

    static public void computeNativeCospi(TestNativeCospi.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = cospi(args.inV, t);
    }

    static public void computeNativeDistance(TestNativeDistance.ArgumentsFloatFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = distance(new float[]{args.inLeftVector}, new float[]{args.inRightVector}, t);
    }

    static public void computeNativeDistance(TestNativeDistance.ArgumentsFloatNFloatNFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = distance(args.inLeftVector, args.inRightVector, t);
    }

    static public void computeNativeDivide(TestNativeDivide.ArgumentsFloatFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = t.divide(t.new32(args.inLeftVector), t.new32(args.inRightVector));
    }

    static public void computeNativeExp(TestNativeExp.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = exp(args.inV, t);
    }

    static public void computeNativeExp10(TestNativeExp10.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = exp10(args.inV, t);
    }

    static public void computeNativeExp2(TestNativeExp2.ArgumentsFloatFloat args, Target t) {
        // TODO we would like to use NATIVE_PRECISION, NATIVE_PRECISION
        t.setPrecision(13000, 13000, true);
        args.out = exp2(args.inV, t);
    }

    static public void computeNativeExpm1(TestNativeExpm1.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = expm1(args.inV, t);
    }

    static public void computeNativeHypot(TestNativeHypot.ArgumentsFloatFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = hypot(args.inA, args.inB, t);
    }

    static public void computeNativeLength(TestNativeLength.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = length(new float[] {args.inV}, t);
    }

    static public void computeNativeLength(TestNativeLength.ArgumentsFloatNFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = length(args.inV, t);
    }

    static public void computeNativeLog(TestNativeLog.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        // For very small values, allow anything.
        if (Math.abs(args.inV) < 1.e-20) {
            args.out = any32(t);
        } else {
            args.out = log(args.inV, t);
        }
    }

    static public void computeNativeLog10(TestNativeLog10.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        // For very small values, allow anything.
        if (Math.abs(args.inV) < 1.e-20) {
            args.out = any32(t);
        } else {
            args.out = log10(args.inV, t);
        }
    }

    static public void computeNativeLog1p(TestNativeLog1p.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = log1p(args.inV, t);
    }

    static public void computeNativeLog2(TestNativeLog2.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        // For very small values, allow anything.
        if (Math.abs(args.inV) < 1.e-20) {
            args.out = any32(t);
        } else {
            args.out = log2(args.inV, t);
        }
    }

    static public void computeNativeNormalize(TestNativeNormalize.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        Target.Floaty[] out = new Target.Floaty[1];
        normalize(new float[] {args.inV}, out, t);
        args.out = out[0];
    }

    static public void computeNativeNormalize(TestNativeNormalize.ArgumentsFloatNFloatN args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        normalize(args.inV, args.out, t);
    }

    static public void computeNativePowr(TestNativePowr.ArgumentsFloatFloatFloat args, Target t) {
        // TODO we would like to use NATIVE_PRECISION, NATIVE_PRECISION
        t.setPrecision(32000, 32000, true);
        // For very small values, allow anything.
        if (Math.abs(args.inBase) < 1.e-20) {
            args.out = any32(t);
        } else {
            args.out = powr(args.inBase, args.inExponent, t);
        }
    }

    static public void computeNativeRecip(TestNativeRecip.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = recip(args.inV, t);
    }

    static public void computeNativeRootn(TestNativeRootn.ArgumentsFloatIntFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        // Allow anything for zero.
        if (args.inN == 0) {
            args.out = any32(t);
        } else {
            args.out = rootn(args.inV, args.inN, t);
        }
    }

    static public void computeNativeRsqrt(TestNativeRsqrt.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = rsqrt(args.inV, t);
    }

    static public void computeNativeSin(TestNativeSin.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = sin(args.inV, t);
    }

    static public void computeNativeSincos(TestNativeSincos.ArgumentsFloatFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.outCos = cos(args.inV, t);
        args.out = sin(args.inV, t);
    }

    static public void computeNativeSinh(TestNativeSinh.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = sinh(args.inV, t);
    }

    static public void computeNativeSinpi(TestNativeSinpi.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = sinpi(args.inV, t);
    }

    static public void computeNativeSqrt(TestNativeSqrt.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = sqrt(args.inV, t);
    }

    static public void computeNativeTan(TestNativeTan.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = tan(args.inV, t);
    }

    static public void computeNativeTanh(TestNativeTanh.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = tanh(args.inV, t);
    }

    static public void computeNativeTanpi(TestNativeTanpi.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(NATIVE_PRECISION, NATIVE_PRECISION, true);
        args.out = tanpi(args.inV, t);
    }

    static public void computeNextafter(TestNextafter.ArgumentsFloatFloatFloat args, Target t) {
        t.setPrecision(0, 0, false);
        args.out = t.new32(Math.nextAfter(args.inV, args.inTarget));
    }

    static public void computeNormalize(TestNormalize.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(1, 1, false);
        Target.Floaty[] out = new Target.Floaty[1];
        normalize(new float[] {args.inV}, out, t);
        args.out = out[0];
    }

    static public void computeNormalize(TestNormalize.ArgumentsFloatNFloatN args, Target t) {
        t.setPrecision(1, 1, false);
        normalize(args.inV, args.out, t);
    }

    static public void computePow(TestPow.ArgumentsFloatFloatFloat args, Target t) {
        t.setPrecision(16, 128, false);
        Target.Floaty base = t.new32(args.inBase);
        Target.Floaty exponent = t.new32(args.inExponent);
        args.out = t.new32(
            pow(base.mid32(), exponent.mid32()),
            pow(base.min32(), exponent.min32()),
            pow(base.min32(), exponent.max32()),
            pow(base.max32(), exponent.min32()),
            pow(base.max32(), exponent.max32()));
    }

    static public void computePown(TestPown.ArgumentsFloatIntFloat args, Target t) {
        t.setPrecision(16, 128, false);
        Target.Floaty in = t.new32(args.inBase);
        // We use double for the calculations because floats does not have enough
        // mantissa bits.  Knowing if an int is odd or even will matter for negative
        // numbers.  Using a float loses the lowest bit.
        final double y = (double) args.inExponent;
        args.out = t.new32(
            (float) Math.pow(in.mid32(), y),
            (float) Math.pow(in.min32(), y),
            (float) Math.pow(in.max32(), y));
    }

    static public void computePowr(TestPowr.ArgumentsFloatFloatFloat args, Target t) {
        t.setPrecision(16, 128, false);
        args.out = powr(args.inBase, args.inExponent, t);
    }

    static public void computeRadians(TestRadians.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(3, 3, false);
        Target.Floaty in = t.new32(args.inV);
        Target.Floaty k = t.new32((float)(Math.PI / 180.0));
        args.out = t.multiply(in, k);
    }

    static public void computeRemainder(TestRemainder.ArgumentsFloatFloatFloat args, Target t) {
        t.setPrecision(0, 0, false);
        RemquoResult result = remquo(args.inNumerator, args.inDenominator);
        args.out = t.new32(result.remainder);
    }

    static public String verifyRemquo(TestRemquo.ArgumentsFloatFloatIntFloat args, Target t) {
        t.setPrecision(0, 0, false);
        RemquoResult expected = remquo(args.inNumerator, args.inDenominator);
        // If the expected remainder is NaN, we don't validate the quotient.  It's because of
        // a division by zero.
        if (expected.remainder != expected.remainder) {
            // Check that the value we got is NaN too.
            if (args.out == args.out) {
                return "Expected a remainder of NaN but got " +  Float.toString(args.out);
            }
        } else {
            // The quotient should have the same lowest three bits.
            if ((args.outQuotient & 0x07) != (expected.quotient & 0x07)) {
                return "Quotient returned " +  Integer.toString(args.outQuotient) +
                    " does not have the same lower three bits as the expected " +
                    Integer.toString(expected.quotient);
            }
            Target.Floaty remainder = t.new32(expected.remainder);
            if (!remainder.couldBe(args.out)) {
                return "Remainder returned " + Float.toString(args.out) +
                    " is not similar to the expected " +
                    remainder.toString();
            }
        }
        return null;
    }

    static public void computeRint(TestRint.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(0, 0, false);
        Target.Floaty in = t.new32(args.inV);
        args.out = t.new32(
            rint(in.mid32()),
            rint(in.min32()),
            rint(in.max32()));
    }

    static public void computeRootn(TestRootn.ArgumentsFloatIntFloat args, Target t) {
        t.setPrecision(16, 16, false);
        args.out = rootn(args.inV, args.inN, t);
    }

    static public void computeRound(TestRound.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(0, 0, false);
        Target.Floaty in = t.new32(args.inV);
        args.out = t.new32(
            round(in.mid32()),
            round(in.min32()),
            round(in.max32()));
    }

    static public void computeRsqrt(TestRsqrt.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(2, 2, false);
        args.out = rsqrt(args.inV, t);
    }

    static public void computeSign(TestSign.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(0, 0, false);
        args.out = t.new32(Math.signum(args.inV));
    }

    static public void computeSin(TestSin.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(4, 128, false);
        args.out = sin(args.inV, t);
    }

    static public void computeSincos(TestSincos.ArgumentsFloatFloatFloat args, Target t) {
        t.setPrecision(4, 128, false);
        args.outCos = cos(args.inV,t );
        args.out = sin(args.inV, t);
    }

    static public void computeSinh(TestSinh.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(4, 128, false);
        args.out = sinh(args.inV, t);
    }

    static public void computeSinpi(TestSinpi.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(4, 128, false);
        args.out = sinpi(args.inV, t);
    }

    static public void computeSqrt(TestSqrt.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(3, 3, false);
        args.out = sqrt(args.inV, t);
    }

    static public void computeStep(TestStep.ArgumentsFloatFloatFloat args, Target t) {
        t.setPrecision(0, 0, false);
        args.out = t.new32(args.inV < args.inEdge ? 0.f : 1.f);
    }

    static public void computeTan(TestTan.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(5, 128, false);
        args.out = tan(args.inV, t);
    }

    static public void computeTanh(TestTanh.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(5, 128, false);
        args.out = tanh(args.inV, t);
    }

    static public void computeTanpi(TestTanpi.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(4, 128, false);
        args.out = tanpi(args.inV, t);
    }

    static public void computeTgamma(TestTgamma.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(16, 128, false);
        Target.Floaty in = t.new32(args.inV);
        args.out = t.new32(
            tgamma(in.mid32()),
            tgamma(in.min32()),
            tgamma(in.max32()));
    }

    static public void computeTrunc(TestTrunc.ArgumentsFloatFloat args, Target t) {
        t.setPrecision(0, 0, false);
        Target.Floaty in = t.new32(args.inV);
        args.out = t.new32(
            trunc(in.mid32()),
            trunc(in.min32()),
            trunc(in.max32()));
    }
}
