package com.wanfadger.AdministrativeareaApi.entity;

/**
 * The {@code name} property, which lives on the six concrete entities rather than on
 * {@link BaseEntity} (it carries different uniqueness rules per level, so it cannot simply be
 * hoisted).
 *
 * <p>Its only purpose is to let generic code bind {@code <T extends BaseEntity & NamedArea>} and
 * so read/write both the shared columns and the name through ONE code path — which is what
 * collapses the six copy-pasted DTO mappers and the six create/update switch branches into one.
 *
 * <p>Lombok's {@code @Getter}/{@code @Setter} on each entity already generate these, so the six
 * entities satisfy this interface by adding {@code implements NamedArea} and nothing else.
 */
public interface NamedArea {

    String getName();

    void setName(String name);
}
