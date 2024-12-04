/*
 * Copyright 2013, Morten Nobel-Joergensen
 *
 * License: The BSD 3-Clause License
 * http://opensource.org/licenses/BSD-3-Clause
 */
package is.galia.processor.resample;

/**
 * @author Heinz Doerr
 */
final class BiCubicHighFreqResponse extends BiCubicFilter {

    BiCubicHighFreqResponse() {
        super(-1.f);
    }

}