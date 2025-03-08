package com.example.demo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CalculatorController {

    @Autowired
    private SoapClient soapClient;

    @GetMapping("/add")
    public String add(@RequestParam int a, @RequestParam int b) {
        return soapClient.callAddOperation(a, b);
    }
}