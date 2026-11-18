package com.osc.backend.github.dto.upstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Raw upstream GitHub API license representation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubLicenseUpstreamDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("key")
    private String key;

    @JsonProperty("name")
    private String name;

    @JsonProperty("spdx_id")
    private String spdxId;

    @JsonProperty("url")
    private String url;

    public GitHubLicenseUpstreamDto() {
    }

    public GitHubLicenseUpstreamDto(String key, String name, String spdxId, String url) {
        this.key = key;
        this.name = name;
        this.spdxId = spdxId;
        this.url = url;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpdxId() {
        return spdxId;
    }

    public void setSpdxId(String spdxId) {
        this.spdxId = spdxId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
