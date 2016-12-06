package edu.unc.cs.robotics.ros;

/**
 * For result of master API getPublishedTopics
 */
public class ROSTopicInfo implements Comparable<ROSTopicInfo> {
    final String _topic;
    final String _dataType;

    public ROSTopicInfo(String topic, String dataType) {
        _topic = topic;
        _dataType = dataType;
    }

    public String getTopic() {
        return _topic;
    }

    public String getDataType() {
        return _dataType;
    }

    @Override
    public int compareTo(ROSTopicInfo o) {
        int c = _topic.compareTo(o._topic);
        if (c != 0) {
            return c;
        }

        return _dataType.compareTo(o._dataType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ROSTopicInfo that = (ROSTopicInfo)o;

        return _topic.equals(that._topic) && _dataType.equals(that._dataType);

    }

    @Override
    public int hashCode() {
        return 31*_topic.hashCode() + _dataType.hashCode();
    }

    @Override
    public String toString() {
        return "TopicInfo(topic="+_topic+", dataType="+ _dataType +")";
    }
}
