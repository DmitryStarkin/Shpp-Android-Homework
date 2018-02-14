/**
 * Copyright © 2017 Dmitry Starkin Contacts: t0506803080@gmail.com. All rights reserved
 *
 */
package com.hplasplas.weather.models.weather.forecast;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Snow {
    
    @SerializedName("3h")
    @Expose
    private Double _3h;
    
    public Double get3h() {
        return _3h;
    }
    
    public void set3h(Double _3h) {
        this._3h = _3h;
    }
}
