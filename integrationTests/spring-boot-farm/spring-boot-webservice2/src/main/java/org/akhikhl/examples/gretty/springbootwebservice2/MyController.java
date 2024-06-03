package org.akhikhl.examples.gretty.springbootwebservice2;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/mycontroller")
public class MyController {
    @RequestMapping(value = "/getdate", method = RequestMethod.POST)
    public Map<String, ?> home() {
        return Collections.singletonMap("date", new SimpleDateFormat("EEE, d MMM yyyy").format(new Date()));
    }
}