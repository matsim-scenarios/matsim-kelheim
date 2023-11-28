package org.matsim.run.prepare.network;
import org.matsim.application.prepare.network.opt.FeatureRegressor;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
    
/**
* Generated model, do not modify.
*/
public final class KelheimNetworkParams_capacity_priority implements FeatureRegressor {
    
    public static KelheimNetworkParams_capacity_priority INSTANCE = new KelheimNetworkParams_capacity_priority();
    public static final double[] DEFAULT_PARAMS = {513.3065, 454.2857, 457.79913, 445.21454, 313.60873, 398.25674, 361.8319, 441.46933, 368.7754, 262.13687, 258.60803, 218.2267, 419.8214, 355.26813, 352.6314, 331.74902, 353.2195, 274.36874, 289.19098, 272.13757, 277.60104, 238.36414, 226.53522, 256.0288, 273.78098, 228.8479, 182.40054, 162.54294, 152.41734, 132.44678, 119.445694, 268.9954, 218.42975, 211.23123, 219.6922, 150.0736, 186.7153, 208.63527, 168.74617, 112.016785, 211.48004, 164.08119, 182.91444, 167.2684, 152.52792, 83.829056, 168.22504, 138.41534, 91.78831, 79.259865, 128.728, 179.74962, 149.7081, 130.0413, 107.51707, 107.50197, 131.7558, 62.5938, 84.974625, 61.58993, 138.90704, 121.28081, 104.15823, 108.69388, 67.25861, 97.6967, 113.66333, 43.866825, 65.722336, 59.6587, 49.245975, 39.86665, 62.44856, 92.679276, 74.49471, 81.312355, 37.71908, 54.964283, 77.03959, 59.73755, 35.651318, 46.61538, 45.93516, 25.37219, 55.704716, 25.612352, 38.261852, 65.17779, 62.26938, 53.004486, 74.0051, 64.8412, 75.4813, 55.417694, 58.762062, 50.494347, 17.388065, 46.50762, 36.10372, 59.653175, 33.366917, 49.107452, 32.572823, -22.693932, 27.476343, 31.75842, 17.033457, 12.3939085, 44.248413, 38.685535, 33.489494, 8.250418, 5.400735, 15.528226, 22.60216, 45.6692, 31.608906, 40.33012, 26.198893, 16.542606, 13.471906, 33.76956, 5.254571, 30.697563, 25.255245, 13.416997, 9.389927, 24.061903, -17.452854, 35.15463, 25.00886, -11.618695, 19.621958, 4.663152, 22.148489, -0.37989056, 26.912107, 17.071007, 22.848114, 41.462498, 17.296383, 24.364145, 17.524658, 11.316603, 4.893713, -3.6525161, 10.50035, 38.940155, 7.521177, -0.014959986, 20.838451, 14.500598, 18.559196, -33.011787, 13.3131, 4.6790547, 12.734893, 16.245182, 12.472162, 32.11076, 21.020697, -12.391058, -24.22375, 14.414696, -2.4003835, -7.228932, 7.5031114, 22.208187, 12.298602, 7.888221, 5.099571, 2.2681546, -0.10557881, 44.64564, 17.884125, -4.144173, 11.553524, -2.7693298, 11.097078, -30.250309, 9.255662, 2.4824646, -29.789434, -19.86918, -5.497463, 18.600313, 3.1126585, 10.832065, -22.08218, 5.055297, -18.583956, 6.75124, 14.559189, 19.981308, 8.259332, 5.0573034, -1.634682, -22.556143, -14.230491, 26.758627, 12.446683, -18.965086, -7.822403, 8.731344, 6.7053905, -8.72763, 18.478937, 4.436338, 6.193743, -16.912682, 2.7308612, 12.748905, 4.955889, 6.0114417, -7.8130383, -2.5964603, -9.427315, 9.514565, 28.62612, 15.873281, 13.216271, -14.008008, -20.424484, -2.7855806, -0.023910655, -31.725637, 28.888523, 0.7319675, 7.4871264, 3.909073, 8.392408, 3.6148007, 20.02056, -41.973106, -5.476211, 7.491202, -2.3267171, -22.292297, -3.626829, 11.019555, -8.323467, 16.730473, 1.9427656, 0.97690845, -6.5138297, 2.7801545, 16.67151, 10.219597, 1.4282844, 11.049184, -6.2674174, 3.7642376, -21.386892, 1.5253811, -11.451429, -10.306692, -1.2854139, 10.9063, 3.048613, -6.5835567, 14.176591, 2.1571164, 6.04242, -18.606346, 0.5235225, -11.286564, -17.094429, 15.040858, 2.2312677, 0.9977561, -28.89311, 1.8043312, 15.389515, -25.827772, 1.373111, 5.732197, 0.14167428, 20.20562, 4.3449183, -3.7816076, -18.564672, -9.731146, -8.326323, 4.922813, -0.21676049, 0.42821464, -22.36384, 5.025358, 12.992834, 2.4203398, -3.7492566, 4.413235, 1.5558748, -32.366978, -5.8277774, 4.2910933, -1.0884621, -32.82358};

    @Override
    public double predict(Object2DoubleMap<String> ft) {
        return predict(ft, DEFAULT_PARAMS);
    }
    
    @Override
    public double[] getData(Object2DoubleMap<String> ft) {
        double[] data = new double[14];
		data[0] = (ft.getDouble("length") - 128.2457324488668) / 113.93955441207002;
		data[1] = (ft.getDouble("speed") - 18.73397457158651) / 5.565821413411222;
		data[2] = (ft.getDouble("num_lanes") - 1.1785516860143725) / 0.4419464243794225;
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
        if (input[1] >= -0.6205687) {
            if (input[3] >= -1.385) {
                if (input[6] >= 3.5) {
                    if (input[1] >= 0.3765887) {
                        var0 = params[0];
                    } else {
                        var0 = params[1];
                    }
                } else {
                    var0 = params[2];
                }
            } else {
                var0 = params[3];
            }
        } else {
            if (input[6] >= 4.5) {
                if (input[6] >= 5.5) {
                    var0 = params[4];
                } else {
                    var0 = params[5];
                }
            } else {
                if (input[7] >= 0.5) {
                    var0 = params[6];
                } else {
                    if (input[1] >= -1.6195228) {
                        var0 = params[7];
                    } else {
                        var0 = params[8];
                    }
                }
            }
        }
        double var1;
        if (input[2] >= 2.9900644) {
            if (input[4] >= -0.5) {
                var1 = params[9];
            } else {
                if (input[1] >= -0.3717285) {
                    var1 = params[10];
                } else {
                    var1 = params[11];
                }
            }
        } else {
            if (input[2] >= 0.7273468) {
                if (input[1] >= -0.12199) {
                    if (input[3] >= -1.385) {
                        var1 = params[12];
                    } else {
                        var1 = params[13];
                    }
                } else {
                    var1 = params[14];
                }
            } else {
                if (input[1] >= -1.6195228) {
                    if (input[5] >= 1.5) {
                        var1 = params[15];
                    } else {
                        var1 = params[16];
                    }
                } else {
                    var1 = params[17];
                }
            }
        }
        double var2;
        if (input[4] >= -0.5) {
            if (input[10] >= 0.5) {
                if (input[1] >= 0.3765887) {
                    if (input[0] >= -0.8611648) {
                        var2 = params[18];
                    } else {
                        var2 = params[19];
                    }
                } else {
                    if (input[1] >= -1.1200457) {
                        var2 = params[20];
                    } else {
                        var2 = params[21];
                    }
                }
            } else {
                if (input[5] >= 1.5) {
                    if (input[0] >= -0.17457268) {
                        var2 = params[22];
                    } else {
                        var2 = params[23];
                    }
                } else {
                    if (input[8] >= 0.5) {
                        var2 = params[24];
                    } else {
                        var2 = params[25];
                    }
                }
            }
        } else {
            var2 = params[26];
        }
        double var3;
        if (input[2] >= 2.9900644) {
            if (input[0] >= -0.8601116) {
                if (input[1] >= -0.12199) {
                    var3 = params[27];
                } else {
                    if (input[4] >= -0.5) {
                        var3 = params[28];
                    } else {
                        var3 = params[29];
                    }
                }
            } else {
                var3 = params[30];
            }
        } else {
            if (input[10] >= 0.5) {
                if (input[1] >= 0.3765887) {
                    if (input[2] >= 0.7273468) {
                        var3 = params[31];
                    } else {
                        var3 = params[32];
                    }
                } else {
                    if (input[0] >= 0.38405687) {
                        var3 = params[33];
                    } else {
                        var3 = params[34];
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[0] >= -0.6081798) {
                        var3 = params[35];
                    } else {
                        var3 = params[36];
                    }
                } else {
                    if (input[1] >= -1.3697842) {
                        var3 = params[37];
                    } else {
                        var3 = params[38];
                    }
                }
            }
        }
        double var4;
        if (input[2] >= 2.9900644) {
            var4 = params[39];
        } else {
            if (input[2] >= 0.7273468) {
                if (input[1] >= -0.12199) {
                    if (input[3] >= -1.385) {
                        var4 = params[40];
                    } else {
                        var4 = params[41];
                    }
                } else {
                    if (input[6] >= 4.5) {
                        var4 = params[42];
                    } else {
                        var4 = params[43];
                    }
                }
            } else {
                if (input[6] >= 3.5) {
                    if (input[3] >= -4.17) {
                        var4 = params[44];
                    } else {
                        var4 = params[45];
                    }
                } else {
                    if (input[1] >= -1.1200457) {
                        var4 = params[46];
                    } else {
                        var4 = params[47];
                    }
                }
            }
        }
        double var5;
        if (input[2] >= 2.9900644) {
            if (input[0] >= -0.80236167) {
                var5 = params[48];
            } else {
                var5 = params[49];
            }
        } else {
            if (input[2] >= 0.7273468) {
                if (input[0] >= 0.47686923) {
                    var5 = params[50];
                } else {
                    if (input[3] >= 6.95) {
                        var5 = params[51];
                    } else {
                        var5 = params[52];
                    }
                }
            } else {
                if (input[6] >= 3.5) {
                    if (input[10] >= 0.5) {
                        var5 = params[53];
                    } else {
                        var5 = params[54];
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var5 = params[55];
                    } else {
                        var5 = params[56];
                    }
                }
            }
        }
        double var6;
        if (input[2] >= 2.9900644) {
            if (input[1] >= -0.6205687) {
                if (input[4] >= -0.5) {
                    var6 = params[57];
                } else {
                    var6 = params[58];
                }
            } else {
                var6 = params[59];
            }
        } else {
            if (input[1] >= -0.6205687) {
                if (input[2] >= 0.7273468) {
                    if (input[1] >= 1.1258043) {
                        var6 = params[60];
                    } else {
                        var6 = params[61];
                    }
                } else {
                    if (input[11] >= 0.5) {
                        var6 = params[62];
                    } else {
                        var6 = params[63];
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    var6 = params[64];
                } else {
                    if (input[8] >= 0.5) {
                        var6 = params[65];
                    } else {
                        var6 = params[66];
                    }
                }
            }
        }
        double var7;
        if (input[2] >= 2.9900644) {
            if (input[1] >= -0.12199) {
                if (input[4] >= -0.5) {
                    var7 = params[67];
                } else {
                    var7 = params[68];
                }
            } else {
                if (input[4] >= -0.5) {
                    var7 = params[69];
                } else {
                    if (input[0] >= -0.63648427) {
                        var7 = params[70];
                    } else {
                        var7 = params[71];
                    }
                }
            }
        } else {
            if (input[1] >= -1.1200457) {
                if (input[2] >= 0.7273468) {
                    if (input[0] >= 1.8557582) {
                        var7 = params[72];
                    } else {
                        var7 = params[73];
                    }
                } else {
                    if (input[3] >= 1.385) {
                        var7 = params[74];
                    } else {
                        var7 = params[75];
                    }
                }
            } else {
                if (input[0] >= -0.30126265) {
                    if (input[3] >= 4.17) {
                        var7 = params[76];
                    } else {
                        var7 = params[77];
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var7 = params[78];
                    } else {
                        var7 = params[79];
                    }
                }
            }
        }
        double var8;
        if (input[2] >= 2.9900644) {
            if (input[6] >= 4.5) {
                if (input[0] >= -0.5692556) {
                    var8 = params[80];
                } else {
                    var8 = params[81];
                }
            } else {
                if (input[5] >= 1.5) {
                    var8 = params[82];
                } else {
                    var8 = params[83];
                }
            }
        } else {
            if (input[5] >= 1.5) {
                if (input[0] >= 0.1255426) {
                    if (input[0] >= 0.53303933) {
                        var8 = params[84];
                    } else {
                        var8 = params[85];
                    }
                } else {
                    if (input[3] >= 1.39) {
                        var8 = params[86];
                    } else {
                        var8 = params[87];
                    }
                }
            } else {
                if (input[0] >= 0.45703417) {
                    if (input[3] >= -4.165) {
                        var8 = params[88];
                    } else {
                        var8 = params[89];
                    }
                } else {
                    if (input[2] >= 0.7273468) {
                        var8 = params[90];
                    } else {
                        var8 = params[91];
                    }
                }
            }
        }
        double var9;
        if (input[1] >= -0.6205687) {
            if (input[3] >= -4.165) {
                if (input[2] >= 0.7273468) {
                    if (input[1] >= 0.3765887) {
                        var9 = params[92];
                    } else {
                        var9 = params[93];
                    }
                } else {
                    if (input[5] >= 1.5) {
                        var9 = params[94];
                    } else {
                        var9 = params[95];
                    }
                }
            } else {
                if (input[13] >= 0.5) {
                    var9 = params[96];
                } else {
                    if (input[3] >= -12.5) {
                        var9 = params[97];
                    } else {
                        var9 = params[98];
                    }
                }
            }
        } else {
            if (input[3] >= -2.78) {
                if (input[5] >= 1.5) {
                    if (input[9] >= 0.5) {
                        var9 = params[99];
                    } else {
                        var9 = params[100];
                    }
                } else {
                    if (input[1] >= -1.1200457) {
                        var9 = params[101];
                    } else {
                        var9 = params[102];
                    }
                }
            } else {
                var9 = params[103];
            }
        }
        double var10;
        if (input[2] >= 2.9900644) {
            if (input[0] >= -0.8315438) {
                if (input[1] >= -0.12199) {
                    var10 = params[104];
                } else {
                    if (input[4] >= -0.5) {
                        var10 = params[105];
                    } else {
                        var10 = params[106];
                    }
                }
            } else {
                var10 = params[107];
            }
        } else {
            if (input[1] >= -1.6195228) {
                if (input[3] >= -1.385) {
                    if (input[1] >= 0.3765887) {
                        var10 = params[108];
                    } else {
                        var10 = params[109];
                    }
                } else {
                    if (input[8] >= 0.5) {
                        var10 = params[110];
                    } else {
                        var10 = params[111];
                    }
                }
            } else {
                if (input[0] >= -0.3034129) {
                    if (input[8] >= 0.5) {
                        var10 = params[112];
                    } else {
                        var10 = params[113];
                    }
                } else {
                    if (input[5] >= 1.5) {
                        var10 = params[114];
                    } else {
                        var10 = params[115];
                    }
                }
            }
        }
        double var11;
        if (input[4] >= -0.5) {
            if (input[10] >= 0.5) {
                if (input[3] >= -4.165) {
                    if (input[8] >= 0.5) {
                        var11 = params[116];
                    } else {
                        var11 = params[117];
                    }
                } else {
                    if (input[3] >= -12.5) {
                        var11 = params[118];
                    } else {
                        var11 = params[119];
                    }
                }
            } else {
                if (input[3] >= 1.385) {
                    if (input[6] >= 2.5) {
                        var11 = params[120];
                    } else {
                        var11 = params[121];
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var11 = params[122];
                    } else {
                        var11 = params[123];
                    }
                }
            }
        } else {
            if (input[6] >= 5.5) {
                var11 = params[124];
            } else {
                if (input[6] >= 4.5) {
                    var11 = params[125];
                } else {
                    var11 = params[126];
                }
            }
        }
        double var12;
        if (input[1] >= -0.6205687) {
            if (input[0] >= -0.86537755) {
                if (input[0] >= 0.467215) {
                    if (input[0] >= 0.5262814) {
                        var12 = params[127];
                    } else {
                        var12 = params[128];
                    }
                } else {
                    if (input[2] >= 0.7273468) {
                        var12 = params[129];
                    } else {
                        var12 = params[130];
                    }
                }
            } else {
                if (input[0] >= -0.87731373) {
                    var12 = params[131];
                } else {
                    if (input[3] >= -6.945) {
                        var12 = params[132];
                    } else {
                        var12 = params[133];
                    }
                }
            }
        } else {
            if (input[0] >= -0.18119898) {
                if (input[5] >= 1.5) {
                    if (input[9] >= 0.5) {
                        var12 = params[134];
                    } else {
                        var12 = params[135];
                    }
                } else {
                    if (input[4] >= 0.5) {
                        var12 = params[136];
                    } else {
                        var12 = params[137];
                    }
                }
            } else {
                if (input[9] >= 0.5) {
                    if (input[2] >= 0.7273468) {
                        var12 = params[138];
                    } else {
                        var12 = params[139];
                    }
                } else {
                    if (input[5] >= 1.5) {
                        var12 = params[140];
                    } else {
                        var12 = params[141];
                    }
                }
            }
        }
        double var13;
        if (input[2] >= 2.9900644) {
            if (input[0] >= -0.77739227) {
                if (input[6] >= 5.5) {
                    var13 = params[142];
                } else {
                    if (input[1] >= -0.12199) {
                        var13 = params[143];
                    } else {
                        var13 = params[144];
                    }
                }
            } else {
                if (input[1] >= -0.6205687) {
                    var13 = params[145];
                } else {
                    var13 = params[146];
                }
            }
        } else {
            if (input[1] >= 0.3765887) {
                if (input[2] >= 0.7273468) {
                    if (input[3] >= -2.775) {
                        var13 = params[147];
                    } else {
                        var13 = params[148];
                    }
                } else {
                    if (input[6] >= 3.5) {
                        var13 = params[149];
                    } else {
                        var13 = params[150];
                    }
                }
            } else {
                if (input[3] >= -5.5550003) {
                    if (input[0] >= 0.4573852) {
                        var13 = params[151];
                    } else {
                        var13 = params[152];
                    }
                } else {
                    var13 = params[153];
                }
            }
        }
        double var14;
        if (input[0] >= -0.859234) {
            if (input[0] >= -0.828472) {
                if (input[6] >= 3.5) {
                    if (input[10] >= 0.5) {
                        var14 = params[154];
                    } else {
                        var14 = params[155];
                    }
                } else {
                    if (input[0] >= 0.4573852) {
                        var14 = params[156];
                    } else {
                        var14 = params[157];
                    }
                }
            } else {
                if (input[6] >= 2.5) {
                    if (input[5] >= 1.5) {
                        var14 = params[158];
                    } else {
                        var14 = params[159];
                    }
                } else {
                    if (input[11] >= 0.5) {
                        var14 = params[160];
                    } else {
                        var14 = params[161];
                    }
                }
            }
        } else {
            if (input[0] >= -0.8835012) {
                if (input[6] >= 3.5) {
                    var14 = params[162];
                } else {
                    if (input[0] >= -0.8673962) {
                        var14 = params[163];
                    } else {
                        var14 = params[164];
                    }
                }
            } else {
                if (input[2] >= 0.7273468) {
                    if (input[0] >= -0.96977496) {
                        var14 = params[165];
                    } else {
                        var14 = params[166];
                    }
                } else {
                    if (input[5] >= 1.5) {
                        var14 = params[167];
                    } else {
                        var14 = params[168];
                    }
                }
            }
        }
        double var15;
        if (input[2] >= 2.9900644) {
            if (input[6] >= 5.5) {
                var15 = params[169];
            } else {
                if (input[5] >= 1.5) {
                    var15 = params[170];
                } else {
                    if (input[4] >= -0.5) {
                        var15 = params[171];
                    } else {
                        var15 = params[172];
                    }
                }
            }
        } else {
            if (input[1] >= -0.6205687) {
                if (input[2] >= 0.7273468) {
                    if (input[3] >= 6.95) {
                        var15 = params[173];
                    } else {
                        var15 = params[174];
                    }
                } else {
                    if (input[9] >= 0.5) {
                        var15 = params[175];
                    } else {
                        var15 = params[176];
                    }
                }
            } else {
                if (input[3] >= -2.78) {
                    if (input[7] >= 0.5) {
                        var15 = params[177];
                    } else {
                        var15 = params[178];
                    }
                } else {
                    var15 = params[179];
                }
            }
        }
        double var16;
        if (input[0] >= -0.86155975) {
            if (input[0] >= 0.4573852) {
                if (input[0] >= 0.48941976) {
                    if (input[0] >= 0.68347) {
                        var16 = params[180];
                    } else {
                        var16 = params[181];
                    }
                } else {
                    if (input[0] >= 0.467215) {
                        var16 = params[182];
                    } else {
                        var16 = params[183];
                    }
                }
            } else {
                if (input[3] >= 12.775) {
                    if (input[13] >= 0.5) {
                        var16 = params[184];
                    } else {
                        var16 = params[185];
                    }
                } else {
                    if (input[2] >= 2.9900644) {
                        var16 = params[186];
                    } else {
                        var16 = params[187];
                    }
                }
            }
        } else {
            if (input[0] >= -0.8835012) {
                if (input[6] >= 3.5) {
                    var16 = params[188];
                } else {
                    if (input[0] >= -0.8747246) {
                        var16 = params[189];
                    } else {
                        var16 = params[190];
                    }
                }
            } else {
                if (input[8] >= 0.5) {
                    if (input[0] >= -1.02261) {
                        var16 = params[191];
                    } else {
                        var16 = params[192];
                    }
                } else {
                    var16 = params[193];
                }
            }
        }
        double var17;
        if (input[0] >= -0.61269975) {
            if (input[0] >= -0.51887804) {
                if (input[1] >= -1.6195228) {
                    if (input[1] >= -0.6205687) {
                        var17 = params[194];
                    } else {
                        var17 = params[195];
                    }
                } else {
                    if (input[5] >= 1.5) {
                        var17 = params[196];
                    } else {
                        var17 = params[197];
                    }
                }
            } else {
                if (input[3] >= 11.115) {
                    var17 = params[198];
                } else {
                    if (input[5] >= 1.5) {
                        var17 = params[199];
                    } else {
                        var17 = params[200];
                    }
                }
            }
        } else {
            if (input[0] >= -0.6191505) {
                var17 = params[201];
            } else {
                if (input[13] >= 0.5) {
                    if (input[6] >= 2.5) {
                        var17 = params[202];
                    } else {
                        var17 = params[203];
                    }
                } else {
                    if (input[3] >= -12.5) {
                        var17 = params[204];
                    } else {
                        var17 = params[205];
                    }
                }
            }
        }
        double var18;
        if (input[4] >= -0.5) {
            if (input[3] >= -12.775) {
                if (input[1] >= 2.6727457) {
                    var18 = params[206];
                } else {
                    if (input[0] >= 0.12132984) {
                        var18 = params[207];
                    } else {
                        var18 = params[208];
                    }
                }
            } else {
                if (input[4] >= 0.5) {
                    var18 = params[209];
                } else {
                    if (input[0] >= 0.0032847896) {
                        var18 = params[210];
                    } else {
                        var18 = params[211];
                    }
                }
            }
        } else {
            if (input[1] >= -0.12199) {
                var18 = params[212];
            } else {
                if (input[6] >= 5.5) {
                    var18 = params[213];
                } else {
                    if (input[0] >= -0.7552314) {
                        var18 = params[214];
                    } else {
                        var18 = params[215];
                    }
                }
            }
        }
        double var19;
        if (input[5] >= 1.5) {
            if (input[9] >= 0.5) {
                if (input[6] >= 3.5) {
                    if (input[3] >= 2.775) {
                        var19 = params[216];
                    } else {
                        var19 = params[217];
                    }
                } else {
                    if (input[0] >= 1.1475757) {
                        var19 = params[218];
                    } else {
                        var19 = params[219];
                    }
                }
            } else {
                if (input[1] >= -0.12199) {
                    if (input[3] >= -2.775) {
                        var19 = params[220];
                    } else {
                        var19 = params[221];
                    }
                } else {
                    if (input[13] >= 0.5) {
                        var19 = params[222];
                    } else {
                        var19 = params[223];
                    }
                }
            }
        } else {
            if (input[9] >= 0.5) {
                if (input[6] >= 3.5) {
                    var19 = params[224];
                } else {
                    var19 = params[225];
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[0] >= -0.5746532) {
                        var19 = params[226];
                    } else {
                        var19 = params[227];
                    }
                } else {
                    if (input[13] >= 0.5) {
                        var19 = params[228];
                    } else {
                        var19 = params[229];
                    }
                }
            }
        }
        double var20;
        if (input[0] >= -0.859234) {
            if (input[3] >= -1.385) {
                if (input[0] >= -0.85603046) {
                    if (input[9] >= 0.5) {
                        var20 = params[230];
                    } else {
                        var20 = params[231];
                    }
                } else {
                    var20 = params[232];
                }
            } else {
                if (input[6] >= 3.5) {
                    if (input[13] >= 0.5) {
                        var20 = params[233];
                    } else {
                        var20 = params[234];
                    }
                } else {
                    if (input[1] >= 0.3765887) {
                        var20 = params[235];
                    } else {
                        var20 = params[236];
                    }
                }
            }
        } else {
            if (input[6] >= 3.5) {
                if (input[0] >= -0.87621665) {
                    var20 = params[237];
                } else {
                    if (input[0] >= -1.0221713) {
                        var20 = params[238];
                    } else {
                        var20 = params[239];
                    }
                }
            } else {
                if (input[0] >= -0.86840546) {
                    var20 = params[240];
                } else {
                    if (input[0] >= -0.8747246) {
                        var20 = params[241];
                    } else {
                        var20 = params[242];
                    }
                }
            }
        }
        double var21;
        if (input[1] >= -0.6205687) {
            if (input[2] >= 0.7273468) {
                if (input[2] >= 2.9900644) {
                    if (input[0] >= -0.6629456) {
                        var21 = params[243];
                    } else {
                        var21 = params[244];
                    }
                } else {
                    if (input[0] >= -0.33531582) {
                        var21 = params[245];
                    } else {
                        var21 = params[246];
                    }
                }
            } else {
                if (input[11] >= 0.5) {
                    if (input[5] >= 1.5) {
                        var21 = params[247];
                    } else {
                        var21 = params[248];
                    }
                } else {
                    if (input[0] >= -0.5955415) {
                        var21 = params[249];
                    } else {
                        var21 = params[250];
                    }
                }
            }
        } else {
            if (input[0] >= -0.17909262) {
                if (input[11] >= 0.5) {
                    if (input[1] >= -1.1200457) {
                        var21 = params[251];
                    } else {
                        var21 = params[252];
                    }
                } else {
                    if (input[0] >= 0.70036495) {
                        var21 = params[253];
                    } else {
                        var21 = params[254];
                    }
                }
            } else {
                if (input[2] >= 0.7273468) {
                    if (input[5] >= 1.5) {
                        var21 = params[255];
                    } else {
                        var21 = params[256];
                    }
                } else {
                    if (input[0] >= -0.3393969) {
                        var21 = params[257];
                    } else {
                        var21 = params[258];
                    }
                }
            }
        }
        double var22;
        if (input[4] >= 0.5) {
            if (input[3] >= -11.115) {
                if (input[13] >= 0.5) {
                    if (input[0] >= 1.5525273) {
                        var22 = params[259];
                    } else {
                        var22 = params[260];
                    }
                } else {
                    if (input[0] >= -0.5194485) {
                        var22 = params[261];
                    } else {
                        var22 = params[262];
                    }
                }
            } else {
                var22 = params[263];
            }
        } else {
            if (input[6] >= 4.5) {
                if (input[2] >= 0.7273468) {
                    if (input[0] >= -0.8590584) {
                        var22 = params[264];
                    } else {
                        var22 = params[265];
                    }
                } else {
                    var22 = params[266];
                }
            } else {
                if (input[1] >= -0.6205687) {
                    if (input[3] >= 9.725) {
                        var22 = params[267];
                    } else {
                        var22 = params[268];
                    }
                } else {
                    if (input[3] >= -2.78) {
                        var22 = params[269];
                    } else {
                        var22 = params[270];
                    }
                }
            }
        }
        double var23;
        if (input[0] >= -0.91439474) {
            if (input[0] >= -0.9037751) {
                if (input[0] >= -0.86155975) {
                    if (input[0] >= -0.85633767) {
                        var23 = params[271];
                    } else {
                        var23 = params[272];
                    }
                } else {
                    if (input[5] >= 1.5) {
                        var23 = params[273];
                    } else {
                        var23 = params[274];
                    }
                }
            } else {
                if (input[11] >= 0.5) {
                    if (input[0] >= -0.91026974) {
                        var23 = params[275];
                    } else {
                        var23 = params[276];
                    }
                } else {
                    var23 = params[277];
                }
            }
        } else {
            if (input[0] >= -0.9280862) {
                if (input[1] >= -0.12199) {
                    if (input[6] >= 2.5) {
                        var23 = params[278];
                    } else {
                        var23 = params[279];
                    }
                } else {
                    if (input[0] >= -0.9262432) {
                        var23 = params[280];
                    } else {
                        var23 = params[281];
                    }
                }
            } else {
                if (input[13] >= 0.5) {
                    var23 = params[282];
                } else {
                    if (input[0] >= -0.95682955) {
                        var23 = params[283];
                    } else {
                        var23 = params[284];
                    }
                }
            }
        }
        double var24;
        if (input[3] >= -1.385) {
            if (input[3] >= 12.775) {
                if (input[13] >= 0.5) {
                    if (input[4] >= 0.5) {
                        var24 = params[285];
                    } else {
                        var24 = params[286];
                    }
                } else {
                    if (input[2] >= 0.7273468) {
                        var24 = params[287];
                    } else {
                        var24 = params[288];
                    }
                }
            } else {
                if (input[6] >= 4.5) {
                    if (input[4] >= 0.5) {
                        var24 = params[289];
                    } else {
                        var24 = params[290];
                    }
                } else {
                    if (input[13] >= 0.5) {
                        var24 = params[291];
                    } else {
                        var24 = params[292];
                    }
                }
            }
        } else {
            if (input[1] >= -0.6205687) {
                if (input[6] >= 3.5) {
                    if (input[13] >= 0.5) {
                        var24 = params[293];
                    } else {
                        var24 = params[294];
                    }
                } else {
                    if (input[1] >= 0.3765887) {
                        var24 = params[295];
                    } else {
                        var24 = params[296];
                    }
                }
            } else {
                var24 = params[297];
            }
        }
        return 0.5 + (var0 + var1 + var2 + var3 + var4 + var5 + var6 + var7 + var8 + var9 + var10 + var11 + var12 + var13 + var14 + var15 + var16 + var17 + var18 + var19 + var20 + var21 + var22 + var23 + var24);
    }
}
