/*
 * Minecraft Forge
 * Copyright (c) 2016-2021.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package ga.ozli.minecraftmods.javafmlpeeled.javafmlmodpeeled;

import net.minecraftforge.eventbus.EventBusErrorMessage;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.IEventListener;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingException;
import net.minecraftforge.fml.ModLoadingStage;
import net.minecraftforge.fml.event.IModBusEvent;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

import static net.minecraftforge.fml.loading.LogMarkers.LOADING;

public class FMLModContainer extends ModContainer
{
    private static final Logger LOGGER = LogManager.getLogger();
    private final ModFileScanData scanResults;
    private final IEventBus eventBus;
    private Object modInstance;
    private final Class<?> modClass;

    public FMLModContainer(IModInfo info, String className, ModFileScanData modFileScanResults, ModuleLayer gameLayer)
    {
        super(info);
        LOGGER.debug(LOADING,"Creating FMLModContainer instance for {}", className);
        this.scanResults = modFileScanResults;
        activityMap.put(ModLoadingStage.CONSTRUCT, this::constructMod);
        this.eventBus = BusBuilder.builder().setExceptionHandler(this::onEventFailed).setTrackPhases(false).markerType(IModBusEvent.class).build();
        this.configHandler = Optional.of(ce->this.eventBus.post(ce.self()));
        final ga.ozli.minecraftmods.javafmlpeeled.javafmlmodpeeled.FMLJavaModLoadingContext contextExtension = new FMLJavaModLoadingContext(this);
        this.contextExtension = () -> contextExtension;
        try
        {
            var layer = gameLayer.findModule(info.getOwningFile().moduleName()).orElseThrow();
            modClass = Class.forName(layer, className);
            LOGGER.trace(LOADING,"Loaded modclass {} with {}", modClass.getName(), modClass.getClassLoader());
        }
        catch (Throwable e)
        {
            LOGGER.error(LOADING, "Failed to load class {}", className, e);
            throw new ModLoadingException(info, ModLoadingStage.CONSTRUCT, "net.minecraftforge.fml.modloading.failedtoloadmodclass", e);
        }
    }

    private void onEventFailed(IEventBus iEventBus, Event event, IEventListener[] iEventListeners, int i, Throwable throwable)
    {
        LOGGER.error(new EventBusErrorMessage(event, i, iEventListeners, throwable));
    }

    private void constructMod()
    {
        try
        {
            LOGGER.trace(LOADING, "Loading mod instance {} of type {}", getModId(), modClass.getName());
            this.modInstance = modClass.getDeclaredConstructor().newInstance();
            LOGGER.trace(LOADING, "Loaded mod instance {} of type {}", getModId(), modClass.getName());
        }
        catch (Throwable e)
        {
            LOGGER.error(LOADING,"Failed to create mod instance. ModID: {}, class {}", getModId(), modClass.getName(), e);
            throw new ModLoadingException(modInfo, ModLoadingStage.CONSTRUCT, "net.minecraftforge.fml.modloading.failedtoloadmod", e, modClass);
        }
        try {
            LOGGER.trace(LOADING, "Injecting Automatic event subscribers for {}", getModId());
            AutomaticEventSubscriber.inject(this, this.scanResults, this.modClass.getClassLoader());
            LOGGER.trace(LOADING, "Completed Automatic event subscribers for {}", getModId());
        } catch (Throwable e) {
            LOGGER.error(LOADING,"Failed to register automatic subscribers. ModID: {}, class {}", getModId(), modClass.getName(), e);
            throw new ModLoadingException(modInfo, ModLoadingStage.CONSTRUCT, "net.minecraftforge.fml.modloading.failedtoloadmod", e, modClass);
        }
    }

    @Override
    public boolean matches(Object mod)
    {
        return mod == modInstance;
    }

    @Override
    public Object getMod()
    {
        return modInstance;
    }

    public IEventBus getEventBus()
    {
        return this.eventBus;
    }

    @Override
    protected <T extends Event & IModBusEvent> void acceptEvent(final T e) {
        try {
            LOGGER.trace(LOADING, "Firing event for modid {} : {}", this.getModId(), e);
            this.eventBus.post(e);
            LOGGER.trace(LOADING, "Fired event for modid {} : {}", this.getModId(), e);
        } catch (Throwable t) {
            LOGGER.error(LOADING,"Caught exception during event {} dispatch for modid {}", e, this.getModId(), t);
            throw new ModLoadingException(modInfo, modLoadingStage, "net.minecraftforge.fml.modloading.errorduringevent", t);
        }
    }
}
