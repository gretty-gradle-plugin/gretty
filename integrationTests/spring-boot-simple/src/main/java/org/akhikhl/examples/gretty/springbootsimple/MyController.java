package org.akhikhl.examples.gretty.springbootsimple;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/mycontroller")
public class MyController {
    @RequestMapping(value = "/getdate", method = RequestMethod.POST)
    public Map<String, ?> home() {
        return Map.of("date", new SimpleDateFormat("EEE, d MMM yyyy").format(new Date()));
    }
}
