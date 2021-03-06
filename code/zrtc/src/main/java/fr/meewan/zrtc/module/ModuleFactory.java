/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module;

import fr.meewan.zrtc.module.command.CommandModule;
import fr.meewan.zrtc.module.output.OutputModule;
import fr.meewan.zrtc.module.permission.PermissionModule;
import fr.meewan.zrtc.module.pgpmodule.PGPModule;

/**
 *
 * @author Meewan
 */
public class ModuleFactory 
{
    public static Module get(String ModuleName)
    {
        switch(ModuleName.toLowerCase())
        {
            case "commandmodule":
                return new CommandModule();
                
            case "output":
            	return new OutputModule();
                
            case "permission":
                return new PermissionModule();
                
            case "pgpmodule":
                return new PGPModule();
                
            default:
                return null;
        }
    }
}
