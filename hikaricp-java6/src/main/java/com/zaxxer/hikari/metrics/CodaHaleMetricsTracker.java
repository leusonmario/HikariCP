/*
 * Copyright (C) 2013,2014 Brett Wooldridge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zaxxer.hikari.metrics;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.CachedGauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.zaxxer.hikari.pool.HikariPool;
import com.zaxxer.hikari.pool.PoolBagEntry;
import com.zaxxer.hikari.util.PoolUtilities;

public final class CodaHaleMetricsTracker extends MetricsTracker
{
   private final Timer connectionObtainTimer;
   private final Histogram connectionUsage;

   public CodaHaleMetricsTracker(final HikariPool pool, final MetricRegistry registry) {
      super(pool);

      connectionObtainTimer = registry.timer(MetricRegistry.name(pool.getConfiguration().getPoolName(), "pool", "Wait"));
      connectionUsage = registry.histogram(MetricRegistry.name(pool.getConfiguration().getPoolName(), "pool", "Usage"));

      registry.register(MetricRegistry.name(pool.getConfiguration().getPoolName(), "pool", "TotalConnections"),
                        new CachedGauge<Integer>(10, TimeUnit.SECONDS) {
                           @Override
                           protected Integer loadValue()
                           {
                              return pool.getTotalConnections();
                           }
                        });

      registry.register(MetricRegistry.name(pool.getConfiguration().getPoolName(), "pool", "IdleConnections"),
                        new CachedGauge<Integer>(10, TimeUnit.SECONDS) {
                           @Override
                           protected Integer loadValue()
                           {
                              return pool.getIdleConnections();
                           }
                        });

      registry.register(MetricRegistry.name(pool.getConfiguration().getPoolName(), "pool", "ActiveConnections"),
                        new CachedGauge<Integer>(10, TimeUnit.SECONDS) {
                           @Override
                           protected Integer loadValue()
                           {
                              return pool.getActiveConnections();
                           }
                        });

      registry.register(MetricRegistry.name(pool.getConfiguration().getPoolName(), "pool", "PendingConnections"),
                        new CachedGauge<Integer>(10, TimeUnit.SECONDS) {
                           @Override
                           protected Integer loadValue()
                           {
                              return pool.getThreadsAwaitingConnection();
                           }
                        });
   }

   /** {@inheritDoc} */
   @Override
   public Context recordConnectionRequest(final long requestTime)
   {
      return new Context(connectionObtainTimer);
   }

   /** {@inheritDoc} */
   @Override
   public void recordConnectionUsage(final PoolBagEntry bagEntry)
   {
      connectionUsage.update(PoolUtilities.elapsedTimeMs(bagEntry.lastOpenTime));
   }

   public Timer getConnectionAcquisitionTimer()
   {
      return connectionObtainTimer;
   }

   public Histogram getConnectionDurationHistogram()
   {
      return connectionUsage;
   }

   public static final class Context extends MetricsContext
   {
      final Timer.Context innerContext;

      Context(Timer timer) {
         innerContext = timer.time();
      }

      /** {@inheritDoc} */
      @Override
      public void stop()
      {
         innerContext.stop();
      }

      /** {@inheritDoc} */
      @Override
      public void setConnectionLastOpen(final PoolBagEntry bagEntry, final long now)
      {
         bagEntry.lastOpenTime = now;
      }
   }
}
