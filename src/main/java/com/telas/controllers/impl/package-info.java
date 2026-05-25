/**
 * REST controllers for the Telas application.
 * <p>
 * <strong>Convention:</strong> domain controllers (clients, monitors, subscriptions, etc.) expose a
 * {@code *Controller} interface for OpenAPI/Swagger and a {@code *ControllerImpl} implementation.
 * <p>
 * <strong>Monitoring controllers</strong> ({@code Monitoring*ControllerImpl}, {@code SmartPlugAdminController},
 * {@code SmartPlugAccountAdminController}) intentionally omit separate interfaces: they are admin/ops
 * endpoints grouped under {@code /monitoring/**} and documented via {@code @Tag} on each class.
 * Authorization is enforced through {@link com.telas.infra.security.services.AuthenticatedUserService}
 * and {@link com.telas.enums.Permission}, not in controller business logic.
 */
package com.telas.controllers.impl;
