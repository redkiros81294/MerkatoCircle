package com.merkatocircle.iqub.web;

/**
 * One member's position on the dashboard's rotation-wheel SVG. Pixel coordinates are
 * computed in Java (DashboardController) rather than in the Thymeleaf template, because
 * trigonometry belongs in a language that has {@code Math.cos}, not in template expressions.
 */
public record WheelNode(double x, double y, String initial, String colorClass, String ariaLabel) {
}
