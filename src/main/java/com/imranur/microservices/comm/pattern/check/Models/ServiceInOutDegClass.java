package com.imranur.microservices.comm.pattern.check.Models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = { "serviceName", "outDeg","inDeg" , "maxDeg", "numberOfClasses"})
public class ServiceInOutDegClass {

    @JsonProperty("serviceName")
    private String serviceName;
    @JsonProperty("outDeg")
    private int outDeg;
    @JsonProperty("inDeg")
    private int inDeg;
    @JsonProperty("maxDeg")
    private int maxDeg;
    @JsonProperty("numberOfClasses")
    private int numberOfClasses;


    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public int getOutDeg() {
        return outDeg;
    }

    public void setOutDeg(int outDeg) {
        this.outDeg = outDeg;
    }

    public int getInDeg() {
        return inDeg;
    }

    public void setInDeg(int inDeg) {
        this.inDeg = inDeg;
    }

    public int getMaxDeg() {
        return maxDeg;
    }

    public void setMaxDeg(int maxDeg) {
        this.maxDeg = maxDeg;
    }

    public int getNumberOfClasses() {
        return numberOfClasses;
    }

    public void setNumberOfClasses(int numberOfClasses) {
        this.numberOfClasses = numberOfClasses;
    }
}
