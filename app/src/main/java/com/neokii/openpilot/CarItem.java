package com.neokii.openpilot;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class CarItem
{
    private Gson gson = new Gson();

    public int battery_temp = -1;
    public int thermal_status = 0;

    // controlsState
    public boolean enabled = false;
    public boolean active = false;
    public int vego = -1;
    public int vcruise = -1;
    public int angle_steer_des = 0;
    public int alert_status = 0;
    public String alert_text1 = null;
    public String alert_text2 = null;

    // carControl.hudControl
    public boolean left_lane_visible;
    public boolean right_lane_visible;

    // radarState
    public boolean lead1_enabled;
    public int lead1_d_rel = -1;

    public CarItem()
    {
        init();
    }

    public void init()
    {
        battery_temp = -1;
        thermal_status = -1;

        enabled = false;
        active = false;
        vego = -1;
        vcruise = -1;
        angle_steer_des = 0;
        alert_status = -1;
        alert_text1 = null;
        alert_text2 = null;

        left_lane_visible = false;
        right_lane_visible = false;

        lead1_enabled = false;
        lead1_d_rel = -1;
    }

    public String getJson(ai.comma.openpilot.cereal.Log.ThermalData.Reader r)
    {
        JsonObject object = new JsonObject();

        if(this.battery_temp != r.getBat())
        {
            this.battery_temp = r.getBat();
            object.addProperty("battery_temp", this.battery_temp);
        }

        if(this.thermal_status != r.getThermalStatus().ordinal())
        {
            this.thermal_status = r.getThermalStatus().ordinal();
            object.addProperty("thermal_status", this.thermal_status);
        }

        if(object.size() > 0)
            return gson.toJson(object);

        return null;
    }

    private JsonObject lastConsrolState = null;

    public String getJson(ai.comma.openpilot.cereal.Log.ControlsState.Reader r)
    {
        JsonObject object = new JsonObject();

        boolean active = r.getActive();
        boolean enabled = r.getEnabled();
        int vego = (int)(r.getVEgo() * 3.6 + 0.5);
        int vcruise = (int)(r.getVCruise() + 0.5);

        object.addProperty("active", active);
        object.addProperty("enabled", enabled);
        object.addProperty("vego", vego);
        object.addProperty("vcruise", vcruise);
        object.addProperty("angle_steer_des", r.getAngleSteersDes());
        object.addProperty("alert_status", r.getAlertStatus().ordinal());

        if(r.hasAlertText1())
            object.addProperty("alert_text1", r.getAlertText1().toString());
        else
            object.addProperty("alert_text1", "");

        if(r.hasAlertText2())
            object.addProperty("alert_text2", r.getAlertText2().toString());
        else
            object.addProperty("alert_text2", "");

        //if(object.size() > 0)
        {
            if(lastConsrolState == null || !lastConsrolState.equals(object))
            {
                lastConsrolState = object;
                return gson.toJson(object);
            }
        }

        return null;
    }

    public String getJson(ai.comma.openpilot.cereal.Car.CarControl.Reader r)
    {
        JsonObject object = new JsonObject();

        if(r.hasHudControl())
        {
            JsonObject hud = new JsonObject();

            if(this.left_lane_visible != r.getHudControl().getLeftLaneVisible())
            {
                this.left_lane_visible = r.getHudControl().getLeftLaneVisible();
                hud.addProperty("left_lane_visible", this.left_lane_visible);
            }

            if(this.right_lane_visible != r.getHudControl().getRightLaneVisible())
            {
                this.right_lane_visible = r.getHudControl().getRightLaneVisible();
                hud.addProperty("right_lane_visible", this.right_lane_visible);
            }

            if(hud.size() > 0)
                object.add("hud", hud);
        }

        if(object.size() > 0)
            return gson.toJson(object);

        return null;
    }

    public String getJson(ai.comma.openpilot.cereal.Log.RadarState.Reader r)
    {
        JsonObject object = new JsonObject();
        if(r.hasLeadOne())
        {
            int lead1_d_rel = (int)r.getLeadOne().getDRel();
            if(!r.getLeadOne().getStatus())
                lead1_d_rel = -1;

            if(this.lead1_d_rel != lead1_d_rel)
            {
                this.lead1_d_rel = lead1_d_rel;
                object.addProperty("lead1_d_rel", this.lead1_d_rel);
            }
        }

        if(object.size() > 0)
            return gson.toJson(object);

        return null;
    }
}
