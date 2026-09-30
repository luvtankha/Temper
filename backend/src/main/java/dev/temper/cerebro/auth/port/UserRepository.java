package dev.temper.cerebro.auth.port;
import java.util.*;
import dev.temper.cerebro.auth.domain.User;
public interface UserRepository {Optional<User> findUser(String id); List<User> findUsers(); void saveUser(User user);}
