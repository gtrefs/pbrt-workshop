package de.gtrefs.coffeeshop.barista;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.*;

@Repository
public interface CoffeeRepository extends JpaRepository<Cup, Long> {

}
