package com.merkatocircle.iqub.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Turns every expected business-rule exception into a redirect back to where the member
 * came from, carrying a flash message the template shows as a banner. Nothing in this app
 * should ever surface a raw stack trace to a member — that's the whole point of this class.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            RoundClosedException.class,
            RoundAlreadyClosedException.class,
            NoEligibleMembersException.class,
            AlreadyPaidException.class,
            DuplicateEmailException.class,
            InvalidBidException.class,
            NotEligibleException.class,
            NotAuthorizedException.class
    })
    public String handleBusinessRuleViolation(RuntimeException ex, HttpServletRequest request,
                                               RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null && !referer.isBlank() ? referer : "/dashboard");
    }

    @ExceptionHandler(PaymentInitiationException.class)
    public String handlePaymentInitiation(PaymentInitiationException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage",
                "We couldn't reach the payment provider just now. Nothing was charged — please try again.");
        return "redirect:/contribute";
    }
}
