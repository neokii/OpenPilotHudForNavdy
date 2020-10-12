package com.neokii.openpilot;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.neokii.openpilot.util.SettingUtil;

import org.capnproto.PrimitiveList;

import ai.comma.openpilot.cereal.Log;

public class CarItem
{
    private float speed_ratio = 1.0f;

    private Gson gson = new Gson();

    public int cpu_temp = -1;
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

    public boolean break_pressed;

    public int path_lprob = -1;
    public int path_rprob = -1;
    public int path_cprob = -1;

    public CarItem()
    {
        init();
    }

    public void init()
    {
        cpu_temp = -1;
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

        break_pressed = false;
        path_lprob = -1;
        path_rprob = -1;
        path_cprob = -1;

        speed_ratio = SettingUtil.getFloat(MainApp.getAppContext(), "speed_ratio", 1.0f);

        if(speed_ratio < 0.5f || speed_ratio > 1.5f)
            speed_ratio = 1.0f;
    }

    private JsonArray getList(PrimitiveList.Float.Reader r)
    {
        JsonArray items = new JsonArray();
        for(int i = 0; i < r.size(); i++)
            items.add(r.get(i));

        return items.size() > 0 ? items : null;
    }

    public String getJson(Log.PathPlan.Reader r)
    {
        JsonObject object = new JsonObject();

        if(this.path_lprob != (int)(r.getLProb()*100))
        {
            this.path_lprob = (int)(r.getLProb()*100);
            object.addProperty("path_lprob", this.path_lprob);
        }

        if(this.path_rprob != (int)(r.getRProb()*100))
        {
            this.path_rprob = (int)(r.getRProb()*100);
            object.addProperty("path_rprob", this.path_rprob);
        }

        if(this.path_cprob != (int)(r.getCProb()*100))
        {
            this.path_cprob = (int)(r.getCProb()*100);
            object.addProperty("path_cprob", this.path_cprob);
        }

        if(r.hasLPoly())
        {
            JsonArray items = getList(r.getLPoly());
            if(items != null)
                object.add("path_lpoly", items);
        }

        if(r.hasRPoly())
        {
            JsonArray items = getList(r.getRPoly());
            if(items != null)
                object.add("path_rpoly", items);
        }

        if(r.hasCPoly())
        {
            JsonArray items = getList(r.getCPoly());
            if(items != null)
                object.add("path_cpoly", items);
        }

        if(object.size() > 0)
            return gson.toJson(object);

        return null;
    }

    // Car.CarState.Reader
    public String getJson(ai.comma.openpilot.cereal.Car.CarState.Reader r)
    {
        JsonObject object = new JsonObject();

        if(this.break_pressed != r.getBrakeLights())
        {
            this.break_pressed = r.getBrakeLights();
            object.addProperty("break_pressed", this.break_pressed);
        }

        if(object.size() > 0)
            return gson.toJson(object);

        return null;
    }

    public String getJson(ai.comma.openpilot.cereal.Log.ThermalData.Reader r)
    {
        JsonObject object = new JsonObject();

        org.capnproto.PrimitiveList.Float.Reader cpus = r.getCpu();

        float temp = 0;
        if(cpus.size() > 0)
        {
            for(int i = 0; i < cpus.size(); i++)
                temp += cpus.get(i);

            temp /= cpus.size();
        }

        int cpuTemp = (int)(temp * 10.f);
        if(this.cpu_temp != cpuTemp)
        {
            this.cpu_temp = cpuTemp;
            object.addProperty("cpu_temp", this.cpu_temp);
        }

        int bat = (int)(r.getBat() * 1000.f);
        if(this.battery_temp != bat)
        {
            this.battery_temp = bat;
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
        int vego = (int)(r.getVEgo() * speed_ratio * 3.6f + 0.5f);
        int vcruise = (int)(r.getVCruise() + 0.5f);

        object.addProperty("active", active);
        object.addProperty("enabled", enabled);
        object.addProperty("vego", vego);
        object.addProperty("vcruise", vcruise);
        object.addProperty("angle_steer_des", r.getAngleSteers());
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
