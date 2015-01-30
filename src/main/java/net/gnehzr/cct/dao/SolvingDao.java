package net.gnehzr.cct.dao;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.hibernate.SessionFactory;

/**
 * <p>
 * <p>
 * Created: 28.01.2015 7:52
 * <p>
 *
 * @author OneHalf
 */
@Singleton
public class SolvingDao extends HibernateDaoSupport {

    @Inject
    public SolvingDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }
}
