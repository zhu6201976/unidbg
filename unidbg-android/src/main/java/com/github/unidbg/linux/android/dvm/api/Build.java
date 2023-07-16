package com.github.unidbg.linux.android.dvm.api;

import com.github.unidbg.arm.backend.BackendException;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.VM;

public class Build extends DvmObject<String> {

//    public static final String FINGERPRINT = "google/blueline/blueline:9/PQ3A.190801.002/5670241:user/release-keys";
    public static final String FINGERPRINT = "huawei/blueline/blueline:9/PQ3A.190801.002/5670241:user/release-keys";

    protected Build(DvmClass objectType, String value) {
        super(objectType, value);
    }
}
