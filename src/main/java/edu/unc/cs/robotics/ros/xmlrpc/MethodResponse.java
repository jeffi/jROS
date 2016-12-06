package edu.unc.cs.robotics.ros.xmlrpc;

import java.util.Map;

/**
 * Created by jeffi on 7/1/16.
 */
class MethodResponse {
    Object result;
    int faultCode;
    String faultString;

    MethodResponse(Object[] params, Object fault) {
        if (fault != null) {
            @SuppressWarnings("unchecked")
            Map<String,Object> faultMap = (Map<String,Object>)fault;
            faultCode = (Integer)faultMap.get("faultCode");
            faultString = (String)faultMap.get("faultString");
        } else {
            result = params[0];
        }
    }
}
