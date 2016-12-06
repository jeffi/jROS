# jROS

This is a partial implementation of ROS (Robotic Operating System) in Java.
You're probably better off using http://wiki.ros.org/rosjava than this project.

This was originally written as a minimally functional subscriber that could listen to published messages.
It has slowly expanded as I needed more functionality.
It seems to work for many scenarios but is mostly undocumented and has at least one major bug
(workaround: Thread.sleep(2000) after creating an action client) that make it unsuitable for
all but a closely monitored labratory setting.
