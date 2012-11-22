/*
 * Copyright 2011, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include<stdio.h>
static void hello_function(const char *ptr){
        printf("%s", ptr);
}
int my_add(int para_x, int para_y){
        return para_x + para_y;
}
int global_z_i;
double global_z_d;
int global_big_z_i[1000];
double global_big_z_d[1000];
static int global_static_z_i;
static double global_static_z_d;
static int global_static_big_z_i[1000];
static double global_static_big_z_d[1000];
int global_z_i_init = 1;
double global_z_d_init = 1.1;
/*extern int extern_z_i;   */
/*extern double extern_z_d;*/
int main(){
        static int local_static_z_i;
        static double local_static_z_d;
        static int local_static_z_i_init = 2;
        static double local_static_z_d_init = 2.2;
        local_static_z_i = local_static_z_i_init;
        local_static_z_d = local_static_z_d_init;
        printf("%d %f\n", local_static_z_i, local_static_z_d);
        printf("%d %f\n", local_static_z_i_init, local_static_z_d_init);
        hello_function("Hello world!1\n");
        hello_function("Hello world!2\n");
        hello_function("Hello world!3\n");
        global_z_i = my_add(1,2);
        global_z_d = 3.3;
        printf("%d %f\n", global_z_i, global_z_d);
        global_big_z_i[100] = 4;
        global_big_z_d[100] = 4.4;
        printf("%d %f\n", global_big_z_i[100], global_big_z_d[100]);
        global_static_z_i = my_add(2,1);
        global_static_z_d = 3.3;
        printf("%d %f\n", global_static_z_i, global_static_z_d);
        int local_z_i = global_static_z_i = global_z_i;
        double local_z_d = global_static_z_d = global_z_d;
        printf("%d %f\n", local_z_i, local_z_d);
        global_static_big_z_i[500] = 5;
        global_static_big_z_d[500] = 5.5;
        printf("%d %f\n", global_static_big_z_i[500], global_static_big_z_d[500]);
        global_z_i_init = 6;
        global_z_d_init = 6.6;
        printf("%d %f\n", global_z_i_init, global_z_d_init);
        /*printf("%d %f\n", extern_z_i, extern_z_d);*/
        return 0;
}
