/*
 * Copyright 2013, Morten Nobel-Joergensen
 *
 * License: The BSD 3-Clause License
 * http://opensource.org/licenses/BSD-3-Clause
 */
package is.galia.processor.resample;

public interface ResampleFilter {

    float apply(float v);

    float getSamplingRadius();

}
