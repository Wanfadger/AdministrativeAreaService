package com.wanfadger.AdministrativeareaApi.dto;

/**
 * Validation group for constraints that apply on CREATE but not on UPDATE.
 *
 * <p>{@link NewAdministrativeAreaDTO} is used for both, and update is a PARTIAL update — the client
 * sends only the fields it wants to change. So a plain {@code @NotBlank} on {@code name} would be
 * correct for create and would break every update that isn't renaming the area. This group is how
 * the two are told apart.
 */
public interface OnCreate {
}
