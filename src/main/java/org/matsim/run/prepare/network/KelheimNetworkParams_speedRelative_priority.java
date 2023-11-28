package org.matsim.run.prepare.network;
import org.matsim.application.prepare.network.opt.FeatureRegressor;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
    
/**
* Generated model, do not modify.
*/
public final class KelheimNetworkParams_speedRelative_priority implements FeatureRegressor {
    
    public static KelheimNetworkParams_speedRelative_priority INSTANCE = new KelheimNetworkParams_speedRelative_priority();
    public static final double[] DEFAULT_PARAMS = {0.14258, 0.18509062, 0.1319892, 0.21723218, 0.21603745, 0.19829217, 0.015799461, 0.116635226, 0.14575444, 0.20327367, 0.16395684, 0.11601545, 0.21640845, 0.08206906, 0.05880511, 0.08429052, 0.11340088, 0.093996845, 0.10620855, 0.086527504, 0.05717409, 0.051208243, 0.08945094, 0.078496225, 0.041707117, 0.025897821, 0.0, 0.0, -0.029575385, 0.042728774, 0.0104034, 0.015885917, 0.052223407, 0.033378717, 0.047087353, 0.058468573, 0.0, 0.06466163, 0.04185065, 0.019468779, 0.030330494, 0.045942016, 0.038289964, -0.019668661, 0.017600726, -0.0004586524, 0.03717461, 0.0056459773, 0.012591909, 0.022549896, 0.0116556445, -4.202666e-05, 0.0030637577, 0.057480868, -0.017231977, 0.0368912, 0.021958511, -0.001558595, -0.028350547, 0.0105668055, 0.03319126, 0.00077598647, 0.008179297, 0.019685771, 0.0, 0.034217786, 0.01681768, 0.0028752259, -0.00017399325, 0.0, 0.016796356, 0.008874396, 0.006204572, -0.0037890784, 0.0044688755, -0.003736771, 0.0, -0.01827357, 0.0029171482, 0.002013008, -0.03561173, -0.046482977, 0.0, -0.0054611256, -0.04145023, -0.006073535, 0.001994865, -0.00336578, 0.0067814775, 0.022733971, -0.030348081, 0.052311413, -0.02217385, 0.0063940543, 0.0, -0.052340824, -0.017130384, 0.0015076201, -0.023615748, 0.009605852, -0.049438596, 0.024081552, -0.011521572, -0.015348021, 0.008094821, 0.036395855, 0.009676987, -0.0012860277, -0.026436528, 0.0017013269, -0.002000469, 0.0, 0.016535543, 0.0, -0.018094791, -0.00031741906, -0.009125446, 0.0028652034, 0.035583634, 0.016262604, 0.00697231, -0.00039586975, 0.022349015, -0.012721074, 0.0020894026, -0.00085663697, -0.0023749138, 0.008718011, -0.0022398487, -0.013597394, -0.0043775383, -0.017085912, 0.013795144, 0.0018055632, -0.00045142087, -0.010426378, 0.005336418, -0.01905197, 0.0, -0.04431133, 0.0, -0.0051804576, -0.0052269953, -0.00048787356, 0.025612522, 0.0006136042, 0.00431704, -0.021402005, 0.02743, 0.008106672, 0.0011297752, -0.019030219, 0.0025165707, -0.01437997, 0.0053062406, 0.0, -0.07729356, -0.0031229793, 0.00018771166, 0.022339622, 0.0, 0.042545404, 0.017232591, -0.006971238, -0.0018871836, 0.017152747, 0.00015398538, -0.008246575, 0.0005927125, 0.0049083773, -0.031346098, -0.0019986308, 0.00038081518, 0.0001680049, 0.021914843, 0.0047407467, -0.033111952, 0.035641335, 0.0, -0.002907388, 0.019804982, -0.045110524, 0.019038578, -0.003974008, -0.031898443, 0.0, 0.022357713, 0.005287226, -0.0010917142, 0.0074089016, 0.0, -6.954866e-05, 0.001980811, -0.009432828, 0.0043549556, -0.007879655, 0.0, -0.011892872, 0.010624668, -0.009519411, 0.00090196956, -0.0006203552, 0.04372789, -6.3213985e-05, -0.03789152, 0.018042292, 0.00035593993, -0.047173694, 0.0, -0.022874609, 0.022519335, -0.023955967, -0.0030863592, 0.0037608324, 0.03155114, -0.04190333, 0.0041914163, 0.005044915, -0.014528895, -0.00050808745, 0.0, 0.00903659, 0.0040342268, -3.4117253e-05, -0.00060050486, 0.005287638, 0.0003903862, 0.011337712, 0.0024241547, -0.001441593, 0.026447205, -0.044049036, 0.034218255, 0.0, -0.037156276, 0.00016951207, -0.01417557, 0.03196794, 0.010758303, -0.02123378, -0.000877532, 0.00095114263, -0.0077428487, -0.00017970474, -0.022118319, 0.011194641, 0.00023148689, -0.0004496356, 0.0015944508, 0.014240748, 0.0139294565, 0.0, -0.036184974, -0.02306282, 0.0, 0.021672152, 0.018900892, 0.0010910452, 0.00053403067, -0.005321817, 0.025602743, -0.0029336503, 0.021603176, -0.03692212, 0.0, 0.022669828, -0.0003404166, -0.017295329, 0.0, 0.025074359, -0.0038062339, -0.0035507346, 0.0043254173, 4.036595e-05, 0.0140129095, 0.0026760506, -0.0092805615, 0.000796777, 0.017925842, -0.041179158, 0.004166777, -0.034036264, -5.0499322e-05, 0.020006966, -0.05467064, -0.0019616303, -1.6677554e-05, -0.030815292, 0.0353502, -0.039857887, 0.010231023, 0.0, -0.017825207, -0.00014596463, 0.0125302905, 0.0062052775, -0.027710754, 0.0, -0.0008782463, 0.0069861487, 0.00059019076, -0.00024371801, 0.009451566, -0.0010030661, 0.003637168, -0.008439712, 0.004958818, 0.007469266, -4.96744e-05, 0.0024510692, -3.743595e-05, 0.03226367, 0.0015893868, 0.029082987, -0.002180701, -0.041853897, -0.00833852, 0.003975689, -0.008420623, 0.007828};

    @Override
    public double predict(Object2DoubleMap<String> ft) {
        return predict(ft, DEFAULT_PARAMS);
    }
    
    @Override
    public double[] getData(Object2DoubleMap<String> ft) {
        double[] data = new double[14];
		data[0] = (ft.getDouble("length") - 119.41859232175503) / 97.6939832841333;
		data[1] = (ft.getDouble("speed") - 14.501608775137111) / 3.818077829017195;
		data[2] = (ft.getDouble("num_lanes") - 1.0317939750417295) / 0.20080167007595912;
		data[3] = ft.getDouble("change_speed");
		data[4] = ft.getDouble("change_num_lanes");
		data[5] = ft.getDouble("num_to_links");
		data[6] = ft.getDouble("junction_inc_lanes");
		data[7] = ft.getDouble("priority_lower");
		data[8] = ft.getDouble("priority_equal");
		data[9] = ft.getDouble("priority_higher");
		data[10] = ft.getDouble("is_secondary_or_higher");
		data[11] = ft.getDouble("is_primary_or_higher");
		data[12] = ft.getDouble("is_motorway");
		data[13] = ft.getDouble("is_link");

        return data;
    }
    
    @Override
    public double predict(Object2DoubleMap<String> ft, double[] params) {

        double[] data = getData(ft);
        for (int i = 0; i < data.length; i++)
            if (Double.isNaN(data[i])) throw new IllegalArgumentException("Invalid data at index: " + i);
    
        return score(data, params);
    }
    public static double score(double[] input, double[] params) {
        double var0;
        if (input[0] >= -0.14083356) {
            if (input[1] >= 2.7496536) {
                if (input[3] >= -11.115) {
                    if (input[1] >= 5.7315207) {
                        var0 = params[0];
                    } else {
                        var0 = params[1];
                    }
                } else {
                    var0 = params[2];
                }
            } else {
                if (input[0] >= 0.4517311) {
                    var0 = params[3];
                } else {
                    if (input[9] >= 0.5) {
                        var0 = params[4];
                    } else {
                        var0 = params[5];
                    }
                }
            }
        } else {
            if (input[9] >= 0.5) {
                if (input[13] >= 0.5) {
                    if (input[3] >= 6.94) {
                        var0 = params[6];
                    } else {
                        var0 = params[7];
                    }
                } else {
                    if (input[1] >= 1.657481) {
                        var0 = params[8];
                    } else {
                        var0 = params[9];
                    }
                }
            } else {
                if (input[1] >= -0.52424514) {
                    if (input[6] >= 1.5) {
                        var0 = params[10];
                    } else {
                        var0 = params[11];
                    }
                } else {
                    if (input[0] >= -1.1161752) {
                        var0 = params[12];
                    } else {
                        var0 = params[13];
                    }
                }
            }
        }
        double var1;
        if (input[0] >= -0.44392288) {
            if (input[6] >= 3.5) {
                if (input[1] >= -0.8883027) {
                    if (input[7] >= 0.5) {
                        var1 = params[14];
                    } else {
                        var1 = params[15];
                    }
                } else {
                    var1 = params[16];
                }
            } else {
                if (input[3] >= -2.775) {
                    if (input[6] >= 2.5) {
                        var1 = params[17];
                    } else {
                        var1 = params[18];
                    }
                } else {
                    if (input[0] >= 1.0626183) {
                        var1 = params[19];
                    } else {
                        var1 = params[20];
                    }
                }
            }
        } else {
            if (input[3] >= -4.165) {
                if (input[5] >= 2.5) {
                    if (input[1] >= -0.8883027) {
                        var1 = params[21];
                    } else {
                        var1 = params[22];
                    }
                } else {
                    if (input[6] >= 1.5) {
                        var1 = params[23];
                    } else {
                        var1 = params[24];
                    }
                }
            } else {
                if (input[3] >= -9.725) {
                    if (input[0] >= -0.9461032) {
                        var1 = params[25];
                    } else {
                        var1 = params[26];
                    }
                } else {
                    if (input[0] >= -0.58031815) {
                        var1 = params[27];
                    } else {
                        var1 = params[28];
                    }
                }
            }
        }
        double var2;
        if (input[0] >= -0.6890761) {
            if (input[9] >= 0.5) {
                if (input[1] >= 1.657481) {
                    if (input[0] >= 1.072189) {
                        var2 = params[29];
                    } else {
                        var2 = params[30];
                    }
                } else {
                    if (input[13] >= 0.5) {
                        var2 = params[31];
                    } else {
                        var2 = params[32];
                    }
                }
            } else {
                if (input[1] >= -0.52424514) {
                    if (input[5] >= 1.5) {
                        var2 = params[33];
                    } else {
                        var2 = params[34];
                    }
                } else {
                    var2 = params[35];
                }
            }
        } else {
            if (input[8] >= 0.5) {
                if (input[11] >= 0.5) {
                    if (input[1] >= 5.004715) {
                        var2 = params[36];
                    } else {
                        var2 = params[37];
                    }
                } else {
                    if (input[3] >= 1.385) {
                        var2 = params[38];
                    } else {
                        var2 = params[39];
                    }
                }
            } else {
                if (input[6] >= 2.5) {
                    if (input[0] >= -0.85080564) {
                        var2 = params[40];
                    } else {
                        var2 = params[41];
                    }
                } else {
                    if (input[0] >= -0.92998147) {
                        var2 = params[42];
                    } else {
                        var2 = params[43];
                    }
                }
            }
        }
        double var3;
        if (input[5] >= 0.5) {
            if (input[7] >= 0.5) {
                if (input[0] >= -0.8769587) {
                    if (input[0] >= 0.04776556) {
                        var3 = params[44];
                    } else {
                        var3 = params[45];
                    }
                } else {
                    if (input[0] >= -1.0921204) {
                        var3 = params[46];
                    } else {
                        var3 = params[47];
                    }
                }
            } else {
                if (input[0] >= -0.83125484) {
                    if (input[5] >= 2.5) {
                        var3 = params[48];
                    } else {
                        var3 = params[49];
                    }
                } else {
                    if (input[0] >= -1.0375624) {
                        var3 = params[50];
                    } else {
                        var3 = params[51];
                    }
                }
            }
        } else {
            if (input[0] >= -0.8460971) {
                if (input[0] >= -0.6176286) {
                    if (input[0] >= -0.5915266) {
                        var3 = params[52];
                    } else {
                        var3 = params[53];
                    }
                } else {
                    var3 = params[54];
                }
            } else {
                var3 = params[55];
            }
        }
        double var4;
        if (input[1] >= -1.2523602) {
            if (input[1] >= 2.7496536) {
                if (input[0] >= 2.692606) {
                    var4 = params[56];
                } else {
                    if (input[10] >= 0.5) {
                        var4 = params[57];
                    } else {
                        var4 = params[58];
                    }
                }
            } else {
                if (input[11] >= 0.5) {
                    if (input[1] >= 0.5666179) {
                        var4 = params[59];
                    } else {
                        var4 = params[60];
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var4 = params[61];
                    } else {
                        var4 = params[62];
                    }
                }
            }
        } else {
            if (input[7] >= 0.5) {
                if (input[3] >= 5.5550003) {
                    if (input[0] >= -0.9114542) {
                        var4 = params[63];
                    } else {
                        var4 = params[64];
                    }
                } else {
                    var4 = params[65];
                }
            } else {
                if (input[3] >= -1.385) {
                    if (input[0] >= -0.88980496) {
                        var4 = params[66];
                    } else {
                        var4 = params[67];
                    }
                } else {
                    var4 = params[68];
                }
            }
        }
        double var5;
        if (input[3] >= -5.835) {
            if (input[0] >= 1.1024365) {
                if (input[6] >= 3.5) {
                    if (input[1] >= 0.93067545) {
                        var5 = params[69];
                    } else {
                        var5 = params[70];
                    }
                } else {
                    var5 = params[71];
                }
            } else {
                if (input[6] >= 3.5) {
                    if (input[9] >= 0.5) {
                        var5 = params[72];
                    } else {
                        var5 = params[73];
                    }
                } else {
                    if (input[6] >= 1.5) {
                        var5 = params[74];
                    } else {
                        var5 = params[75];
                    }
                }
            }
        } else {
            if (input[0] >= 0.36948445) {
                if (input[8] >= 0.5) {
                    if (input[0] >= 2.1608949) {
                        var5 = params[76];
                    } else {
                        var5 = params[77];
                    }
                } else {
                    var5 = params[78];
                }
            } else {
                if (input[3] >= -9.725) {
                    if (input[0] >= -0.55790126) {
                        var5 = params[79];
                    } else {
                        var5 = params[80];
                    }
                } else {
                    var5 = params[81];
                }
            }
        }
        double var6;
        if (input[5] >= 1.5) {
            if (input[3] >= 12.5) {
                if (input[9] >= 0.5) {
                    var6 = params[82];
                } else {
                    if (input[0] >= 0.17617674) {
                        var6 = params[83];
                    } else {
                        var6 = params[84];
                    }
                }
            } else {
                if (input[0] >= -0.600432) {
                    if (input[1] >= 1.657481) {
                        var6 = params[85];
                    } else {
                        var6 = params[86];
                    }
                } else {
                    if (input[1] >= -0.52424514) {
                        var6 = params[87];
                    } else {
                        var6 = params[88];
                    }
                }
            }
        } else {
            if (input[3] >= 6.9449997) {
                if (input[0] >= -0.89000964) {
                    if (input[0] >= -0.81518424) {
                        var6 = params[89];
                    } else {
                        var6 = params[90];
                    }
                } else {
                    var6 = params[91];
                }
            } else {
                if (input[0] >= -1.1474462) {
                    if (input[1] >= 5.7315207) {
                        var6 = params[92];
                    } else {
                        var6 = params[93];
                    }
                } else {
                    if (input[1] >= 0.20386994) {
                        var6 = params[94];
                    } else {
                        var6 = params[95];
                    }
                }
            }
        }
        double var7;
        if (input[9] >= 0.5) {
            if (input[6] >= 2.5) {
                if (input[0] >= -0.48609537) {
                    if (input[13] >= 0.5) {
                        var7 = params[96];
                    } else {
                        var7 = params[97];
                    }
                } else {
                    if (input[13] >= 0.5) {
                        var7 = params[98];
                    } else {
                        var7 = params[99];
                    }
                }
            } else {
                if (input[10] >= 0.5) {
                    var7 = params[100];
                } else {
                    if (input[13] >= 0.5) {
                        var7 = params[101];
                    } else {
                        var7 = params[102];
                    }
                }
            }
        } else {
            if (input[11] >= 0.5) {
                if (input[1] >= 5.7315207) {
                    if (input[0] >= -0.92870194) {
                        var7 = params[103];
                    } else {
                        var7 = params[104];
                    }
                } else {
                    if (input[6] >= 4.5) {
                        var7 = params[105];
                    } else {
                        var7 = params[106];
                    }
                }
            } else {
                if (input[1] >= 2.7496536) {
                    if (input[0] >= 0.67226666) {
                        var7 = params[107];
                    } else {
                        var7 = params[108];
                    }
                } else {
                    if (input[0] >= -0.4920323) {
                        var7 = params[109];
                    } else {
                        var7 = params[110];
                    }
                }
            }
        }
        double var8;
        if (input[6] >= 4.5) {
            if (input[11] >= 0.5) {
                if (input[1] >= 0.5666179) {
                    var8 = params[111];
                } else {
                    var8 = params[112];
                }
            } else {
                if (input[6] >= 5.5) {
                    var8 = params[113];
                } else {
                    var8 = params[114];
                }
            }
        } else {
            if (input[5] >= 1.5) {
                if (input[1] >= -1.2523602) {
                    if (input[6] >= 2.5) {
                        var8 = params[115];
                    } else {
                        var8 = params[116];
                    }
                } else {
                    if (input[6] >= 2.5) {
                        var8 = params[117];
                    } else {
                        var8 = params[118];
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    var8 = params[119];
                } else {
                    if (input[10] >= 0.5) {
                        var8 = params[120];
                    } else {
                        var8 = params[121];
                    }
                }
            }
        }
        double var9;
        if (input[3] >= -4.165) {
            if (input[5] >= 1.5) {
                if (input[3] >= 12.5) {
                    if (input[2] >= 2.3316839) {
                        var9 = params[122];
                    } else {
                        var9 = params[123];
                    }
                } else {
                    if (input[0] >= 0.18615688) {
                        var9 = params[124];
                    } else {
                        var9 = params[125];
                    }
                }
            } else {
                if (input[5] >= 0.5) {
                    if (input[0] >= -0.12532596) {
                        var9 = params[126];
                    } else {
                        var9 = params[127];
                    }
                } else {
                    if (input[0] >= -0.74568146) {
                        var9 = params[128];
                    } else {
                        var9 = params[129];
                    }
                }
            }
        } else {
            if (input[0] >= -0.5294962) {
                var9 = params[130];
            } else {
                var9 = params[131];
            }
        }
        double var10;
        if (input[3] >= -1.385) {
            if (input[4] >= -0.5) {
                if (input[3] >= 16.939999) {
                    var10 = params[132];
                } else {
                    if (input[9] >= 0.5) {
                        var10 = params[133];
                    } else {
                        var10 = params[134];
                    }
                }
            } else {
                if (input[5] >= 1.5) {
                    if (input[9] >= 0.5) {
                        var10 = params[135];
                    } else {
                        var10 = params[136];
                    }
                } else {
                    var10 = params[137];
                }
            }
        } else {
            if (input[2] >= 2.3316839) {
                var10 = params[138];
            } else {
                if (input[13] >= 0.5) {
                    if (input[3] >= -4.165) {
                        var10 = params[139];
                    } else {
                        var10 = params[140];
                    }
                } else {
                    var10 = params[141];
                }
            }
        }
        double var11;
        if (input[1] >= -1.2523602) {
            if (input[0] >= -1.1089075) {
                if (input[0] >= -0.6573956) {
                    if (input[1] >= 1.657481) {
                        var11 = params[142];
                    } else {
                        var11 = params[143];
                    }
                } else {
                    if (input[0] >= -0.66527736) {
                        var11 = params[144];
                    } else {
                        var11 = params[145];
                    }
                }
            } else {
                if (input[6] >= 1.5) {
                    if (input[7] >= 0.5) {
                        var11 = params[146];
                    } else {
                        var11 = params[147];
                    }
                } else {
                    var11 = params[148];
                }
            }
        } else {
            if (input[0] >= -0.9253752) {
                if (input[7] >= 0.5) {
                    var11 = params[149];
                } else {
                    var11 = params[150];
                }
            } else {
                if (input[0] >= -1.0066495) {
                    var11 = params[151];
                } else {
                    var11 = params[152];
                }
            }
        }
        double var12;
        if (input[6] >= 2.5) {
            if (input[13] >= 0.5) {
                if (input[0] >= -0.6392778) {
                    if (input[0] >= -0.45948166) {
                        var12 = params[153];
                    } else {
                        var12 = params[154];
                    }
                } else {
                    if (input[5] >= 1.5) {
                        var12 = params[155];
                    } else {
                        var12 = params[156];
                    }
                }
            } else {
                if (input[0] >= -0.9717957) {
                    if (input[7] >= 0.5) {
                        var12 = params[157];
                    } else {
                        var12 = params[158];
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var12 = params[159];
                    } else {
                        var12 = params[160];
                    }
                }
            }
        } else {
            if (input[13] >= 0.5) {
                if (input[5] >= 1.5) {
                    var12 = params[161];
                } else {
                    if (input[0] >= -0.8991198) {
                        var12 = params[162];
                    } else {
                        var12 = params[163];
                    }
                }
            } else {
                if (input[3] >= 1.39) {
                    if (input[0] >= 0.42895585) {
                        var12 = params[164];
                    } else {
                        var12 = params[165];
                    }
                } else {
                    if (input[0] >= -0.9781932) {
                        var12 = params[166];
                    } else {
                        var12 = params[167];
                    }
                }
            }
        }
        double var13;
        if (input[6] >= 3.5) {
            if (input[9] >= 0.5) {
                var13 = params[168];
            } else {
                if (input[11] >= 0.5) {
                    var13 = params[169];
                } else {
                    if (input[1] >= 2.7496536) {
                        var13 = params[170];
                    } else {
                        var13 = params[171];
                    }
                }
            }
        } else {
            var13 = params[172];
        }
        double var14;
        if (input[0] >= -0.7982947) {
            if (input[0] >= -0.77070856) {
                if (input[0] >= -0.7670748) {
                    if (input[0] >= -0.7615473) {
                        var14 = params[173];
                    } else {
                        var14 = params[174];
                    }
                } else {
                    if (input[5] >= 2.5) {
                        var14 = params[175];
                    } else {
                        var14 = params[176];
                    }
                }
            } else {
                if (input[0] >= -0.7811494) {
                    if (input[1] >= -0.8883027) {
                        var14 = params[177];
                    } else {
                        var14 = params[178];
                    }
                } else {
                    if (input[0] >= -0.7919484) {
                        var14 = params[179];
                    } else {
                        var14 = params[180];
                    }
                }
            }
        } else {
            if (input[5] >= 1.5) {
                if (input[0] >= -0.8000349) {
                    var14 = params[181];
                } else {
                    if (input[6] >= 5.5) {
                        var14 = params[182];
                    } else {
                        var14 = params[183];
                    }
                }
            } else {
                if (input[13] >= 0.5) {
                    if (input[6] >= 2.5) {
                        var14 = params[184];
                    } else {
                        var14 = params[185];
                    }
                } else {
                    if (input[6] >= 2.5) {
                        var14 = params[186];
                    } else {
                        var14 = params[187];
                    }
                }
            }
        }
        double var15;
        if (input[0] >= 1.5917194) {
            if (input[9] >= 0.5) {
                var15 = params[188];
            } else {
                if (input[6] >= 2.5) {
                    var15 = params[189];
                } else {
                    var15 = params[190];
                }
            }
        } else {
            if (input[0] >= -0.14984128) {
                if (input[0] >= -0.031768512) {
                    var15 = params[191];
                } else {
                    if (input[10] >= 0.5) {
                        var15 = params[192];
                    } else {
                        var15 = params[193];
                    }
                }
            } else {
                if (input[0] >= -0.35707003) {
                    var15 = params[194];
                } else {
                    if (input[1] >= 2.7496536) {
                        var15 = params[195];
                    } else {
                        var15 = params[196];
                    }
                }
            }
        }
        double var16;
        if (input[0] >= -1.1677648) {
            if (input[4] >= 0.5) {
                if (input[5] >= 1.5) {
                    var16 = params[197];
                } else {
                    var16 = params[198];
                }
            } else {
                if (input[9] >= 0.5) {
                    if (input[5] >= 3.5) {
                        var16 = params[199];
                    } else {
                        var16 = params[200];
                    }
                } else {
                    var16 = params[201];
                }
            }
        } else {
            var16 = params[202];
        }
        double var17;
        if (input[0] >= -0.9551109) {
            if (input[0] >= -0.9322846) {
                if (input[0] >= -0.8822815) {
                    if (input[0] >= -0.8812067) {
                        var17 = params[203];
                    } else {
                        var17 = params[204];
                    }
                } else {
                    if (input[0] >= -0.8922616) {
                        var17 = params[205];
                    } else {
                        var17 = params[206];
                    }
                }
            } else {
                if (input[0] >= -0.93781203) {
                    var17 = params[207];
                } else {
                    if (input[0] >= -0.9455402) {
                        var17 = params[208];
                    } else {
                        var17 = params[209];
                    }
                }
            }
        } else {
            if (input[10] >= 0.5) {
                if (input[0] >= -0.9711815) {
                    var17 = params[210];
                } else {
                    if (input[0] >= -1.0200074) {
                        var17 = params[211];
                    } else {
                        var17 = params[212];
                    }
                }
            } else {
                if (input[0] >= -0.9867915) {
                    if (input[0] >= -0.9780909) {
                        var17 = params[213];
                    } else {
                        var17 = params[214];
                    }
                } else {
                    if (input[0] >= -0.98761034) {
                        var17 = params[215];
                    } else {
                        var17 = params[216];
                    }
                }
            }
        }
        double var18;
        if (input[3] >= 2.775) {
            if (input[6] >= 2.5) {
                if (input[1] >= -1.2523602) {
                    if (input[4] >= 0.5) {
                        var18 = params[217];
                    } else {
                        var18 = params[218];
                    }
                } else {
                    var18 = params[219];
                }
            } else {
                if (input[0] >= -0.34151125) {
                    var18 = params[220];
                } else {
                    var18 = params[221];
                }
            }
        } else {
            if (input[1] >= -0.52424514) {
                if (input[0] >= 0.17899166) {
                    if (input[6] >= 3.5) {
                        var18 = params[222];
                    } else {
                        var18 = params[223];
                    }
                } else {
                    var18 = params[224];
                }
            } else {
                var18 = params[225];
            }
        }
        double var19;
        if (input[0] >= -1.1412535) {
            if (input[0] >= -1.1236986) {
                if (input[0] >= -0.35707003) {
                    if (input[0] >= -0.34048763) {
                        var19 = params[226];
                    } else {
                        var19 = params[227];
                    }
                } else {
                    if (input[9] >= 0.5) {
                        var19 = params[228];
                    } else {
                        var19 = params[229];
                    }
                }
            } else {
                if (input[0] >= -1.1333716) {
                    var19 = params[230];
                } else {
                    if (input[0] >= -1.1368008) {
                        var19 = params[231];
                    } else {
                        var19 = params[232];
                    }
                }
            }
        } else {
            if (input[8] >= 0.5) {
                var19 = params[233];
            } else {
                var19 = params[234];
            }
        }
        double var20;
        if (input[3] >= 9.725) {
            if (input[8] >= 0.5) {
                if (input[0] >= -1.0671444) {
                    if (input[0] >= -0.7170717) {
                        var20 = params[235];
                    } else {
                        var20 = params[236];
                    }
                } else {
                    var20 = params[237];
                }
            } else {
                var20 = params[238];
            }
        } else {
            if (input[3] >= 5.5550003) {
                if (input[1] >= -1.2523602) {
                    if (input[5] >= 1.5) {
                        var20 = params[239];
                    } else {
                        var20 = params[240];
                    }
                } else {
                    if (input[0] >= -0.7064774) {
                        var20 = params[241];
                    } else {
                        var20 = params[242];
                    }
                }
            } else {
                if (input[0] >= -0.8110386) {
                    if (input[0] >= -0.80392456) {
                        var20 = params[243];
                    } else {
                        var20 = params[244];
                    }
                } else {
                    if (input[0] >= -0.83125484) {
                        var20 = params[245];
                    } else {
                        var20 = params[246];
                    }
                }
            }
        }
        double var21;
        if (input[0] >= -0.5254018) {
            if (input[0] >= -0.5145004) {
                if (input[0] >= -0.45789504) {
                    var21 = params[247];
                } else {
                    if (input[6] >= 2.5) {
                        var21 = params[248];
                    } else {
                        var21 = params[249];
                    }
                }
            } else {
                if (input[10] >= 0.5) {
                    var21 = params[250];
                } else {
                    if (input[9] >= 0.5) {
                        var21 = params[251];
                    } else {
                        var21 = params[252];
                    }
                }
            }
        } else {
            if (input[5] >= 3.5) {
                if (input[1] >= -0.8883027) {
                    var21 = params[253];
                } else {
                    var21 = params[254];
                }
            } else {
                if (input[0] >= -0.5291891) {
                    var21 = params[255];
                } else {
                    if (input[6] >= 5.5) {
                        var21 = params[256];
                    } else {
                        var21 = params[257];
                    }
                }
            }
        }
        double var22;
        if (input[5] >= 0.5) {
            if (input[0] >= -1.1492375) {
                var22 = params[258];
            } else {
                if (input[10] >= 0.5) {
                    var22 = params[259];
                } else {
                    var22 = params[260];
                }
            }
        } else {
            if (input[0] >= -0.965347) {
                if (input[0] >= -0.7927161) {
                    if (input[0] >= -0.7713227) {
                        var22 = params[261];
                    } else {
                        var22 = params[262];
                    }
                } else {
                    if (input[0] >= -0.8361681) {
                        var22 = params[263];
                    } else {
                        var22 = params[264];
                    }
                }
            } else {
                var22 = params[265];
            }
        }
        double var23;
        if (input[1] >= 0.93067545) {
            if (input[6] >= 2.5) {
                if (input[8] >= 0.5) {
                    if (input[1] >= 2.7496536) {
                        var23 = params[266];
                    } else {
                        var23 = params[267];
                    }
                } else {
                    if (input[5] >= 1.5) {
                        var23 = params[268];
                    } else {
                        var23 = params[269];
                    }
                }
            } else {
                var23 = params[270];
            }
        } else {
            if (input[5] >= 1.5) {
                if (input[10] >= 0.5) {
                    if (input[9] >= 0.5) {
                        var23 = params[271];
                    } else {
                        var23 = params[272];
                    }
                } else {
                    var23 = params[273];
                }
            } else {
                if (input[10] >= 0.5) {
                    if (input[6] >= 2.5) {
                        var23 = params[274];
                    } else {
                        var23 = params[275];
                    }
                } else {
                    if (input[6] >= 2.5) {
                        var23 = params[276];
                    } else {
                        var23 = params[277];
                    }
                }
            }
        }
        double var24;
        if (input[12] >= 0.5) {
            if (input[0] >= 0.6799437) {
                var24 = params[278];
            } else {
                if (input[0] >= 0.5878705) {
                    var24 = params[279];
                } else {
                    var24 = params[280];
                }
            }
        } else {
            if (input[1] >= 5.004715) {
                var24 = params[281];
            } else {
                if (input[0] >= -0.99508274) {
                    if (input[0] >= -0.986075) {
                        var24 = params[282];
                    } else {
                        var24 = params[283];
                    }
                } else {
                    if (input[0] >= -0.99651575) {
                        var24 = params[284];
                    } else {
                        var24 = params[285];
                    }
                }
            }
        }
        double var25;
        if (input[0] >= -1.067861) {
            if (input[0] >= -1.0651484) {
                var25 = params[286];
            } else {
                var25 = params[287];
            }
        } else {
            if (input[0] >= -1.0717506) {
                var25 = params[288];
            } else {
                if (input[0] >= -1.0727743) {
                    var25 = params[289];
                } else {
                    if (input[0] >= -1.091711) {
                        var25 = params[290];
                    } else {
                        var25 = params[291];
                    }
                }
            }
        }
        double var26;
        if (input[0] >= -0.7166623) {
            if (input[6] >= 6.5) {
                var26 = params[292];
            } else {
                if (input[0] >= -0.7060168) {
                    var26 = params[293];
                } else {
                    var26 = params[294];
                }
            }
        } else {
            if (input[6] >= 5.5) {
                var26 = params[295];
            } else {
                if (input[0] >= -0.7297132) {
                    if (input[5] >= 1.5) {
                        var26 = params[296];
                    } else {
                        var26 = params[297];
                    }
                } else {
                    var26 = params[298];
                }
            }
        }
        double var27;
        if (input[0] >= 1.5917194) {
            if (input[6] >= 3.5) {
                var27 = params[299];
            } else {
                var27 = params[300];
            }
        } else {
            var27 = params[301];
        }
        double var28;
        if (input[4] >= 1.5) {
            var28 = params[302];
        } else {
            if (input[9] >= 0.5) {
                if (input[0] >= -0.8853011) {
                    if (input[0] >= -0.4875284) {
                        var28 = params[303];
                    } else {
                        var28 = params[304];
                    }
                } else {
                    if (input[0] >= -1.0842898) {
                        var28 = params[305];
                    } else {
                        var28 = params[306];
                    }
                }
            } else {
                if (input[0] >= 3.4271958) {
                    var28 = params[307];
                } else {
                    if (input[0] >= -0.89528126) {
                        var28 = params[308];
                    } else {
                        var28 = params[309];
                    }
                }
            }
        }
        double var29;
        if (input[0] >= -0.9800869) {
            if (input[0] >= -0.9780909) {
                if (input[0] >= -0.9223044) {
                    var29 = params[310];
                } else {
                    if (input[1] >= 2.7496536) {
                        var29 = params[311];
                    } else {
                        var29 = params[312];
                    }
                }
            } else {
                var29 = params[313];
            }
        } else {
            if (input[0] >= -0.9878663) {
                if (input[6] >= 2.5) {
                    var29 = params[314];
                } else {
                    var29 = params[315];
                }
            } else {
                if (input[6] >= 2.5) {
                    if (input[10] >= 0.5) {
                        var29 = params[316];
                    } else {
                        var29 = params[317];
                    }
                } else {
                    if (input[6] >= 1.5) {
                        var29 = params[318];
                    } else {
                        var29 = params[319];
                    }
                }
            }
        }
        return 0.5 + (var0 + var1 + var2 + var3 + var4 + var5 + var6 + var7 + var8 + var9 + var10 + var11 + var12 + var13 + var14 + var15 + var16 + var17 + var18 + var19 + var20 + var21 + var22 + var23 + var24 + var25 + var26 + var27 + var28 + var29);
    }
}
