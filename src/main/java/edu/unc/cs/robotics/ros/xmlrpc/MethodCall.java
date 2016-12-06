package edu.unc.cs.robotics.ros.xmlrpc;

class MethodCall {
    private String _methodName;
    private Object[] _params;

    MethodCall(String methodName, Object[] params) {
        _methodName = methodName;
        _params = params;
    }

    public String getMethodName() {
        return _methodName;
    }

    public Object[] getParams() {
        return _params;
    }
}
