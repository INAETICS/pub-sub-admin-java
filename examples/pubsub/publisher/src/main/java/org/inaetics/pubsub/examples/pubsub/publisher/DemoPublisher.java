/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *******************************************************************************/
package org.inaetics.pubsub.examples.pubsub.publisher;

import org.inaetics.pubsub.api.Publisher;
import org.inaetics.pubsub.examples.pubsub.common.Location;
import org.inaetics.pubsub.examples.pubsub.common.PointOfInterrest;

public class DemoPublisher {

    private volatile Publisher<PointOfInterrest> publisher;

    private Location loc;
    private PointOfInterrest poi;

    Thread thread = thread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                sendLocationUpdate();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    });

    public DemoPublisher() {
        loc = new Location();
        poi = new PointOfInterrest();
        poi.setName("Test");
        poi.setLocation(loc);
    }

    private void sendLocationUpdate() {
        loc.setLat(loc.getLat() + 5.0);
        loc.setLon(loc.getLon() + 3.0);
        //System.out.printf("Sending loc [%s,%s]\n", loc.getLat(), loc.getLon());
        System.out.printf("Sending poi [%s, %s,%s]\n", poi.getName(), poi.getLocation().getLat(), poi.getLocation().getLon());
        //publisher.send(loc);
        publisher.send(poi);
    }

    public void start(){
        thread.start();
    }

    public void stop(){
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
