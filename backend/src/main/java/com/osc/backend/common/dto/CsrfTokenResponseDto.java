package com.osc.backend.common.dto;

/**
 * Data transfer object exposing active CSRF token details to the browser client.
 */
public class CsrfTokenResponseDto {

    private String headerName;
    private String parameterName;
    private String token;

    public CsrfTokenResponseDto() {
    }

    public CsrfTokenResponseDto(String headerName, String parameterName, String token) {
        this.headerName = headerName;
        this.parameterName = parameterName;
        this.token = token;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
