package com.erpcom.erpcom.common.reference;

import java.util.List;
import com.erpcom.erpcom.common.reference.ReferenceModels.*;

public interface ReferenceDataService {
    List<CodeInfo> codes(long userId, String group, String language);
    Bootstrap bootstrap(long userId, String language);
}
