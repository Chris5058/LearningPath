package com.example.demo;
package com.example.demo;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Add")
public class AddRequest {

    private int intA;
    private int intB;

    @XmlElement(name = "intA")
    public int getIntA() {
        return intA;
    }

    public void setIntA(int intA) {
        this.intA = intA;
    }

    @XmlElement(name = "intB")
    public int getIntB() {
        return intB;
    }

    public void setIntB(int intB) {
        this.intB = intB;
    }
}