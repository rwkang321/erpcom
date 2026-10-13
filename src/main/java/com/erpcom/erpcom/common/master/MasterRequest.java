package com.erpcom.erpcom.common.master;

import java.util.Map;
import jakarta.validation.constraints.*;

public record MasterRequest(
        @NotNull @Size(min = 1, max = 40) Map<String, Object> values,
        @Min(0) Long expectedVersion) {}
