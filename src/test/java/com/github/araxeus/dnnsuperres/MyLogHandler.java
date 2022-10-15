package com.github.araxeus.dnnsuperres;/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.util.function.BiConsumer;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;


/**
 * com.github.araxeus.dnnsuperres.MyLogHandler.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-10-14 nsano initial version <br>
 */
public final class MyLogHandler extends Handler {

    private BiConsumer<Level, String> appendable;

    private final Formatter formatter = new SimpleFormatter();

    public MyLogHandler(BiConsumer<Level, String> appendable) {
        this.appendable = appendable;
    }

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        String msg;
        try {
            msg = formatter.format(record);
        } catch (Exception ex) {
            reportError(null, ex, ErrorManager.FORMAT_FAILURE);
            return;
        }

        try {
            appendable.accept(record.getLevel(), msg);
        } catch (Exception ex) {
            reportError(null, ex, ErrorManager.WRITE_FAILURE);
        }
    }

    @Override
    public boolean isLoggable(LogRecord record) {
        if (appendable == null || record == null) {
            return false;
        }
        return super.isLoggable(record);
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }
}
