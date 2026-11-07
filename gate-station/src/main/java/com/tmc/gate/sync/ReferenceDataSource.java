package com.tmc.gate.sync;

import com.tmc.gate.validation.ReferenceData;

public interface ReferenceDataSource {

    ReferenceData fetch();
}
