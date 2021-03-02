package de.gtrefs.coffeeshop.barista;

import java.io.*;

import org.hibernate.engine.spi.*;
import org.hibernate.id.*;

public class UseGivenIdOrElseGenerateIt extends IdentityGenerator {

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) {
        Serializable id = session.getEntityPersister(null, object).getClassMetadata().getIdentifier(object, session);
        return id != null ? id : super.generate(session, object);
    }
}
