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
import java.util.Arrays;
import junit.framework.Assert;

/**
 * This class and the enclosed Floaty class are used to validate the precision of the floating
 * point operations of the various drivers.  Instances of Target contains information about the
 * expectations we have for the functions being tested.  There's an instance of Floaty for each
 * floating value being verified.
 */
public class Target {
    /**
     * In relaxed precision mode, we allow:
     * - less precision in the computation
     * - using only normalized values
     * - reordering of the operations
     * - different rounding mode
     */
    private boolean mIsRelaxedPrecision;

    /*
     * The following two fields are set just before the expected values are computed for a specific
     * RenderScript function.  Hence, we can't use one instance of this class to test two APIs
     * in parallel.  The generated Test*.java code uses one instance of Target per test, so we're
     * safe.
     */

    /**
     * For native, we allow the same things as relaxed precision, plus:
     * - operations don't have to return +/- infinity
     */
    private boolean mIsNative;

    /**
     * How much we'll allow the values tested to diverge from the values
     * we compute.  This can be very large for native_* and half_* tests.
     */
    private int mUlpFactor;

    Target(boolean relaxed) {
        mIsRelaxedPrecision = relaxed;
    }

    /**
     * Sets whether we are testing a native_* function and how many ulp we allow
     * for full and relaxed precision.
     */
    void setPrecision(int fullUlpFactor, int relaxedUlpFactor, boolean isNative) {
        mIsNative = isNative;
        mUlpFactor = mIsRelaxedPrecision ? relaxedUlpFactor : fullUlpFactor;
    }

    /**
     * Helper functions to create a new 32 bit Floaty with the current expected level of precision.
     * We have variations that expect one to five arguments.  Any of the passed arguments are considered
     * valid values for that Floaty.
     */
    Floaty new32(float a) {
        return new Floaty(32, new double [] { a });
    }

    Floaty new32(float a, float b) {
        return new Floaty(32, new double [] { a, b });
    }

    Floaty new32(float a, float b, float c) {
        return new Floaty(32, new double [] { a, b, c });
    }

    Floaty new32(float a, float b, float c, float d) {
        return new Floaty(32, new double [] { a, b, c, d });
    }

    Floaty new32(float a, float b, float c, float d, float e) {
        return new Floaty(32, new double [] { a, b, c, d, e });
    }

    /**
     * Helper functions to create a new 64 bit Floaty with the current expected level of precision.
     * We have variations that expect one to five arguments.  Any of the passed arguments are considered
     * valid values for that Floaty.
     */
    Floaty new64(double a) {
        return new Floaty(64, new double [] { a });
    }

    Floaty new64(double a, double b) {
        return new Floaty(64, new double [] { a, b });
    }

    Floaty new64(double a, double b, double c) {
        return new Floaty(64, new double [] { a, b, c });
    }

    Floaty new64(double a, double b, double c, double d) {
        return new Floaty(64, new double [] { a, b, c, d });
    }

    Floaty new64(double a, double b, double c, double d, double e) {
        return new Floaty(64, new double [] { a, b, c, d, e });
    }

    /**
     * Returns a Floaty that contain a NaN for the specified size.
     */
    Floaty newNan(int numberOfBits) {
        return new Floaty(numberOfBits, new double [] { Double.NaN });
    }

    Floaty add(Floaty a, Floaty b) {
        //Log.w("Target.add", "a: " + a.toString());
        //Log.w("Target.add", "b: " + b.toString());
        assert(a.mNumberOfBits == b.mNumberOfBits);
        if (!a.mHasRange || !b.mHasRange) {
            return newNan(a.mNumberOfBits);
        }
        return new Floaty(a.mNumberOfBits, new double[] { a.mValue + b.mValue,
                                                          a.mMinValue + b.mMinValue,
                                                          a.mMaxValue + b.mMaxValue });
    }

    Floaty subtract(Floaty a, Floaty b) {
        //Log.w("Target.subtract", "a: " + a.toString());
        //Log.w("Target.subtract", "b: " + b.toString());
        assert(a.mNumberOfBits == b.mNumberOfBits);
        if (!a.mHasRange || !b.mHasRange) {
            return newNan(a.mNumberOfBits);
        }
        return new Floaty(a.mNumberOfBits, new double[] { a.mValue - b.mValue,
                                                          a.mMinValue - b.mMaxValue,
                                                          a.mMaxValue - b.mMinValue });
    }

    Floaty multiply(Floaty a, Floaty b) {
        //Log.w("Target.multiply", "a: " + a.toString());
        //Log.w("Target.multiply", "b: " + b.toString());
        assert(a.mNumberOfBits == b.mNumberOfBits);
        if (!a.mHasRange || !b.mHasRange) {
            return newNan(a.mNumberOfBits);
        }
        return new Floaty(a.mNumberOfBits, new double[] { a.mValue * b.mValue,
                                                          a.mMinValue * b.mMinValue,
                                                          a.mMinValue * b.mMaxValue,
                                                          a.mMaxValue * b.mMinValue,
                                                          a.mMaxValue * b.mMaxValue});
    }

    Floaty divide(Floaty a, Floaty b) {
        //Log.w("Target.divide", "a: " + a.toString());
        //Log.w("Target.divide", "b: " + b.toString());
        assert(a.mNumberOfBits == b.mNumberOfBits);
        if (!a.mHasRange || !b.mHasRange) {
            return newNan(a.mNumberOfBits);
        }
        return new Floaty(a.mNumberOfBits, new double[] { a.mValue / b.mValue,
                                                          a.mMinValue / b.mMinValue,
                                                          a.mMinValue / b.mMaxValue,
                                                          a.mMaxValue / b.mMinValue,
                                                          a.mMaxValue / b.mMaxValue});
    }

    /** Returns the absolute value of a Floaty. */
    Floaty abs(Floaty a) {
        if (!a.mHasRange) {
            return newNan(a.mNumberOfBits);
        }
        if (a.mMinValue >= 0 && a.mMaxValue >= 0) {
            // Two non-negatives, no change
            return a;
        }
        Floaty f = new Floaty(a);
        f.mValue = Math.abs(a.mValue);
        if (a.mMinValue < 0 && a.mMaxValue < 0) {
            // Two negatives, we invert
            f.mMinValue = -a.mMaxValue;
            f.mMaxValue = -a.mMinValue;
        } else {
            // We have one negative, one positive.
            f.mMinValue = 0.f;
            f.mMaxValue = Math.max(-a.mMinValue, a.mMaxValue);
        }
        return f;
    }

    /** Returns the square root of a Floaty. */
    Floaty sqrt(Floaty a) {
        //Log.w("Target.sqrt", "a: " + a.toString());
        if (!a.mHasRange) {
            return newNan(a.mNumberOfBits);
        }
        double f = Math.sqrt(a.mValue);
        double min = Math.sqrt(a.mMinValue);
        double max = Math.sqrt(a.mMaxValue);
        double[] values;
        /* If the range of inputs covers 0, make sure we have it as one of
         * the answers, to set the correct lowest bound, as the square root
         * of the negative inputs will yield a NaN flag and won't affect the
         * range.
         */
        if (a.mMinValue < 0 && a.mMaxValue > 0) {
            values = new double[]{f, 0., min, max};
        } else {
            values = new double[]{f, min, max};
        }
        Floaty answer = new Floaty(a.mNumberOfBits, values);
        // Allow a little more imprecision for a square root operation.
        answer.ExpandRangeByUlpFactor();
        return answer;
    }

    /**
     * This class represents the range of floating point values we accept as the result of a
     * computation performed by a runtime driver.
     */
    class Floaty {
        /**
         * The number of bits the value should have, either 32 or 64.  It would have been nice to
         * use generics, e.g. Floaty<double> and Floaty<double> but Java does not support generics
         * of float and double.  Also, Java does not have an f16 type.  This can simulate it,
         * although more work will be needed.
         */
        private int mNumberOfBits;
        /** True if NaN is an acceptable value. */
        private boolean mCanBeNan;
        /**
         * True if mValue, mMinValue, mMaxValue have been set.  This should be the case if mCanBeNan is false.
         * It's possible for both mCanBeNan and mHasRange to be true at the same time.
         */
        private boolean mHasRange;
        /**
         * The typical value we would expect.  We don't just keep track of the min and max
         * of the ranges of values allowed because some functions we are evaluating are
         * discontinuous, e.g. sqrt around 0, lgamma around -1, -2, -3 and tan around pi/2.
         * By keeping track of the middle value, we're more likely to handle this discontinuity
         * correctly.
         */
        private double mValue;
        /** The minimum value we would expect. */
        private double mMinValue;
        /** The maximum value we would expect. */
        private double mMaxValue;

        Floaty(Floaty a) {
            mNumberOfBits = a.mNumberOfBits;
            mCanBeNan = a.mCanBeNan;
            mHasRange = a.mHasRange;
            mValue = a.mValue;
            mMinValue = a.mMinValue;
            mMaxValue = a.mMaxValue;
        }

        /**
         * Creates a Floaty and initializes it so that the values passed could be represented by it.
         * We also expand what's allowed by +/- mUlpFactor to allow for the various rounding modes.
         * values[0] is treated as the representative case, otherwise the order of values does not matter.
         */
        Floaty(int numberOfBits, double values[]) {
            //Log.w("Floaty(double[], ulp)", "input: " + Arrays.toString(values) + ", ulp " + Integer.toString(mUlpFactor));
            mNumberOfBits = numberOfBits;
            mCanBeNan = false;
            mHasRange = false;
            mValue = values[0];
            for (double f: values) {
                if (f != f) {
                    mCanBeNan = true;
                    continue;
                }
                updateMinAndMax(f);
                // For relaxed mode, we don't require support of subnormal values.
                // If we have a subnormal value, we'll allow both the normalized value and zero,
                // to cover the two ways this small value might be handled.
                if (mIsRelaxedPrecision || mIsNative) {
                    if (IsSubnormal(f)) {
                        updateMinAndMax(0.f);
                        updateMinAndMax(smallestNormal(f));
                    }
                }
            }
            // Expand the range by one ulp factor to cover for the different rounding modes.
            ExpandRangeByUlpFactor();
            //Log.w("Floaty(double[], ulp)", "output: " +  toString());
        }

        /** Modify the mMinValue and mMaxValue so that f is contained within the range. */
        private void updateMinAndMax(double f) {
            if (mHasRange) {
                if (f < mMinValue) {
                    mMinValue = f;
                }
                if (f > mMaxValue) {
                    mMaxValue = f;
                }
            } else {
                mHasRange = true;
                mMinValue = f;
                mMaxValue = f;
            }
        }

        /** Modify mMinValue and mMaxValue to allow one extra ulp factor of error on each side. */
        void ExpandRangeByUlpFactor() {
            if (mHasRange && mUlpFactor > 0) {
                // Expand the edges by the specified factor.
                ExpandMin(mUlpFactor);
                ExpandMax(mUlpFactor);
            }
        }

        /** Expand the mMinValue by the number of ulp specified. */
        private void ExpandMin(int ulpFactor) {
            //Log.w("ExpandMin", java.lang.Double.toString(mMinValue) + " by " + Integer.toString(ulpFactor));
            if (!mHasRange) {
                return;
            }
            if (mMinValue == Double.NEGATIVE_INFINITY ||
                mMinValue == Double.POSITIVE_INFINITY) {
                // Can't get any larger
                //Log.w("ExpandMin", "infinity");
                return;
            }
            double ulp = NegativeUlp();
            double delta = ulp * ulpFactor;
            double newValue = mMinValue + delta;
            /*
             * Reduce mMinValue but don't go negative if it's positive because the rounding error
             * we're simulating won't change the sign.
             */
            if (newValue < 0 && mMinValue > 0.f) {
                mMinValue = 0.f;
            } else {
                mMinValue = newValue;
            }
            // If subnormal, also allow the normalized value if it's smaller.
            if ((mIsRelaxedPrecision || mIsNative) && IsSubnormal(mMinValue)) {
                if (mMinValue < 0) {
                    mMinValue = smallestNormal(-1.0f);
                } else {
                    mMinValue = 0.f;
                }
            }
            //Log.w("ExpandMin", "ulp " + java.lang.Double.toString(ulp) + ", delta " + java.lang.Double.toString(delta) + " for " + java.lang.Double.toString(mMinValue));
        }

        /** Expand the mMaxValue by the number of ulp specified. */
        private void ExpandMax(int ulpFactor) {
            //Log.w("ExpandMax", java.lang.Double.toString(mMaxValue) + " by " + Integer.toString(ulpFactor));
            if (!mHasRange) {
                return;
            }
            if (mMaxValue == Double.NEGATIVE_INFINITY ||
                mMaxValue == Double.POSITIVE_INFINITY) {
                // Can't get any larger
                //Log.w("ExpandMax", "infinity");
                return;
            }
            double ulp = Ulp();
            double delta = ulp * ulpFactor;
            double newValue = mMaxValue + delta;
            /*
             * Increase mMaxValue but don't go positive if it's negative because the rounding error
             * we're simulating won't change the sign.
             */
            if (newValue > 0 && mMaxValue < 0.f) {
                mMaxValue = 0.f;
            } else {
                mMaxValue = newValue;
            }
            // If subnormal, also allow the normalized value if it's smaller.
            if ((mIsRelaxedPrecision || mIsNative) && IsSubnormal(mMaxValue)) {
                if (mMaxValue > 0) {
                    mMaxValue = smallestNormal(1.0f);
                } else {
                    mMaxValue = 0.f;
                }
            }
            //Log.w("ExpandMax", "ulp " + java.lang.Double.toString(ulp) + ", delta " + java.lang.Double.toString(delta) + " for " + java.lang.Double.toString(mMaxValue));
        }

        /**
         * Returns true if f is smaller than the smallest normalized number that can be represented
         * by the number of bits we have.
         */
        private boolean IsSubnormal(double f) {
            double af = Math.abs(f);
            return 0 < af && af < smallestNormal(1.0f);
        }

        /**
         * Returns the smallest normal representable by the number of bits we have, of the same
         * sign as f.
         */
        private double smallestNormal(double f) {
            double answer = (mNumberOfBits == 32) ? Float.MIN_NORMAL : Double.MIN_NORMAL;
            if (f < 0) {
                answer = -answer;
            }
            return answer;
        }

        /** Returns the unit of least precision for the maximum value allowed. */
        private double Ulp() {
            double u;
            if (mNumberOfBits == 32) {
                u = Math.ulp((float)mMaxValue);
            } else {
                u = Math.ulp(mMaxValue);
            }
            if (mIsRelaxedPrecision || mIsNative) {
                u = Math.max(u, smallestNormal(1.f));
            }
            return u;
        }

        /** Returns the negative of the unit of least precision for the minimum value allowed. */
        private double NegativeUlp() {
            double u;
            if (mNumberOfBits == 32) {
                u = -Math.ulp((float)mMinValue);
            } else {
                u = -Math.ulp(mMinValue);
            }
            if (mIsRelaxedPrecision || mIsNative) {
                u = Math.min(u, smallestNormal(-1.f));
            }
            return u;
        }

        /** Returns true if the number passed is among the values that are represented by this Floaty. */
        public boolean couldBe(double a) {
            return couldBe(a, 0.0);
        }

        /**
         * Returns true if the number passed is among the values that are represented by this Floaty.
         * An extra amount of allowed error can be specified.
         */
        public boolean couldBe(double a, double extraAllowedError) {
            //Log.w("Floaty.couldBe", "Can " + Double.toString(a) + " be " + toString() + "? ");
            // Handle the input being a NaN.
            if (a != a) {
                //Log.w("couldBe", "true because is Naan");
                return mCanBeNan;
            }
            // If the input is not a NaN, we better have a range.
            if (!mHasRange) {
                return false;
            }
            // Handle the simple case.
            if ((mMinValue - extraAllowedError) <= a && a <= (mMaxValue + extraAllowedError)) {
                return true;
            }
            // For native, we don't require returning +/- infinity.  If that's what we expect,
            // allow all answers.
            if (mIsNative) {
                if (mMinValue == Double.NEGATIVE_INFINITY &&
                    mMaxValue == Double.NEGATIVE_INFINITY) {
                    return true;
                }
                if (mMinValue == Double.POSITIVE_INFINITY &&
                    mMaxValue == Double.POSITIVE_INFINITY) {
                    return true;
                }
            }
            return false;
        }


        public double get64() { return mValue; }
        public double min64() { return mMinValue; }
        public double max64() { return mMaxValue; }
        /**
         * Returns mValue unless zero could be a legal value.  In that case, return it.
         * This is useful for testing functions where the behavior at zero is unusual,
         * e.g. reciprocals.  If mValue is already +0.0 or -0.0, don't change it, to
         * preserve the sign.
         */
        public double mid64() {
            if (mMinValue < 0.0 && mMaxValue > 0.0 && mValue != 0.0) {
                return 0.0;
            }
            return mValue;
        }

        public float get32() { return (float) mValue; }
        public float min32() { return (float) mMinValue; }
        public float max32() { return (float) mMaxValue; }
        public float mid32() { return (float) mid64(); }

        public String toString() {
            String s = String.format("[f%d: ", mNumberOfBits);
            if (mCanBeNan) {
                s += "NaN, ";
            }
            if (mHasRange) {
                if (mNumberOfBits == 32) {
                    float min = (float)mMinValue;
                    float mid = (float)mValue;
                    float max = (float)mMaxValue;
                    s += String.format("%11.9g {%08x} to %11.9g {%08x} to %11.9g {%08x}", min,
                                       Float.floatToRawIntBits(min), mid,
                                       Float.floatToRawIntBits(mid), max,
                                       Float.floatToRawIntBits(max));
                } else {
                    s += String.format("%24.9g {%16x} to %24.9g {%16x} to %24.9g {%16x}", mMinValue,
                                       Double.doubleToRawLongBits(mMinValue), mValue,
                                       Double.doubleToRawLongBits(mValue), mMaxValue,
                                       Double.doubleToRawLongBits(mMaxValue));
                }
            }
            s += "]";
            return s;
        }
    }
}
