package com.cse444.threadtrace.passport.web;

import com.cse444.threadtrace.passport.projection.PublicPassportView;
import com.cse444.threadtrace.passport.proxy.PassportAccessPort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/passports")
public class PassportWebController {

    private final PassportAccessPort passportAccessPort;

    public PassportWebController(PassportAccessPort passportAccessPort) {
        this.passportAccessPort = passportAccessPort;
    }

    @GetMapping("/{qrToken}")
    public String showPublicPassport(
            @PathVariable String qrToken,
            @RequestParam(required = false) Integer version,
            Model model
    ) {
        PublicPassportView passport = passportAccessPort.getPublicPassport(qrToken, version);
        model.addAttribute("passport", passport);
        model.addAttribute("qrToken", qrToken);
        model.addAttribute("version", version);
        return "passport-view";
    }
}
