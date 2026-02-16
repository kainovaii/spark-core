package fr.kainovaii.obsidian.security.user;

public interface UserDetails
{
    Object getId();
    String getUsername();
    String getPassword();
    String getRole();

    default boolean isEnabled() { return true; }
}
