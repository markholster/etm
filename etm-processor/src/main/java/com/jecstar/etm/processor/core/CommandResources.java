package com.jecstar.etm.processor.core;

import com.jecstar.etm.processor.TelemetryCommand.CommandType;
import com.jecstar.etm.server.core.domain.ImportProfile;

import java.io.Closeable;

public interface CommandResources extends Closeable {

    <T> T getPersister(CommandType commandType);

    ImportProfile loadImportProfile(String importProfileName);

}
