package com.github.huangp.entityunit.maker;

import java.util.Date;

/**
 * @author Patrick Huang
 */
class DateMaker implements Maker<Date> {
    @Override
    public Date value() {
        return new Date();
    }
}
